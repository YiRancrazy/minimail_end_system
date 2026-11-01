package com.yirancrazy.minimall.cart.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.cart.constant.CartCodeEnum;
import com.yirancrazy.minimall.cart.dto.CartItemAddDto;
import com.yirancrazy.minimall.cart.dto.CartUpdateDto;
import com.yirancrazy.minimall.cart.entity.CartItemPo;
import com.yirancrazy.minimall.cart.gateway.CartRemoteGateway;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.cart.vo.CartItemVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 协调购物车持久化、商品和用户远程调用以及 MinIO URL 解析。实现类负责记录归属校验，
 *     使用数据库原子 upsert 处理并发添加，在快照缺失时降级为部分视图，并在本地事务中执行结算清理。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final CartItemManager cartItemManager;
    private final CartRemoteGateway cartRemoteGateway;
    private final MinioUtil minioUtil;

    /**
     * 创建购物车应用服务.
     *
     * @param cartItemManager 购物车持久化边界
     * @param cartRemoteGateway 商品和用户远程访问边界
     * @param minioUtil 商品图片公网 URL 解析器
     */
    public CartServiceImpl(
            CartItemManager cartItemManager, CartRemoteGateway cartRemoteGateway, MinioUtil minioUtil) {
        this.cartItemManager = cartItemManager;
        this.cartRemoteGateway = cartRemoteGateway;
        this.minioUtil = minioUtil;
    }

    /**
     * 查询指定用户购物车，并通过批量 SKU、SPU 快照补充记录.
     *
     * <p>购物车为空时不发起远程调用。重复 SPU 主键只解析一次；快照缺失时保留购物车记录，并让
     * 对应视图字段保持 null。
     *
     * @param userId 购物车所属用户
     * @return 按数据层顺序返回的购物车视图
     */
    @Override
    public List<CartItemVo> listByUser(Long userId) {
        List<CartItemPo> items =
                cartItemManager.list(
                        Wrappers.lambdaQuery(CartItemPo.class).eq(CartItemPo::getUserId, userId));
        if (items.isEmpty()) {
            return List.of();
        }
        // 批量快照替代逐条目 2N 次远程调用：1 次批量 SKU + 按需 1 次批量 SPU；缺失快照沿用原降级逻辑
        List<Long> skuIds = items.stream().map(CartItemPo::getSkuId).filter(Objects::nonNull).toList();
        Map<Long, SkuSnapshotDto> skuMap = cartRemoteGateway.batchSkuSnapshot(skuIds).getData();
        List<Long> spuIds =
                skuMap == null
                        ? List.of()
                        : skuMap.values().stream()
                                .map(SkuSnapshotDto::getSpuId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList();
        Map<Long, SpuSnapshotDto> spuMap =
                spuIds.isEmpty() ? Map.of() : cartRemoteGateway.batchSpuSnapshot(spuIds).getData();
        Map<Long, SkuSnapshotDto> effectiveSkuMap = skuMap == null ? Map.of() : skuMap;
        Map<Long, SpuSnapshotDto> effectiveSpuMap = spuMap == null ? Map.of() : spuMap;
        return items.stream()
                .map(
                        item -> {
                            SkuSnapshotDto sku = effectiveSkuMap.get(item.getSkuId());
                            SpuSnapshotDto spu =
                                    sku == null || sku.getSpuId() == null
                                            ? null
                                            : effectiveSpuMap.get(sku.getSpuId());
                            return CartItemVo.from(item, sku, spu, minioUtil);
                        })
                .toList();
    }

    /**
     * 原子插入或累加用户购物车记录.
     *
     * <p>selected 为空时默认已勾选。Mapper 将数量上限控制为 999，且只在首次插入时应用勾选值，
     * 因此添加已有 SKU 会保留当前勾选状态。
     *
     * @param userId 购物车所属用户
     * @param dto SKU 和数量请求
     * @return 持久化购物车记录主键
     * @throws BizException upsert 未影响记录或无法读取结果时抛出
     */
    @Override
    public Long add(Long userId, CartItemAddDto dto) {
        int selected = dto.getSelected() != null ? dto.getSelected() : 1;
        // 唯一键冲突由数据库原子累加，避免 read-check-write 丢增量或首次并发插入异常
        int affected =
                cartItemManager.upsertQuantity(
                        IdWorker.getId(), userId, dto.getSkuId(), dto.getQuantity(), selected);
        if (affected <= 0) {
            log.error("购物车商品 upsert 未影响任何行，userId={}，skuId={}", userId, dto.getSkuId());
            throw new BizException(CommonCode.SYS_ERROR, "购物车更新失败");
        }
        CartItemPo item = cartItemManager.getByUserIdAndSkuId(userId, dto.getSkuId());
        if (item == null) {
            log.error("购物车商品 upsert 成功但无法读取记录，userId={}，skuId={}", userId, dto.getSkuId());
            throw new BizException(CommonCode.SYS_ERROR, "购物车更新失败");
        }
        return item.getId();
    }

    /**
     * 校验归属后删除一条购物车记录.
     *
     * @param id 购物车记录主键
     * @param userId 预期所属用户
     * @return 删除影响记录时返回 true
     * @throws BizException 记录不存在或属于其他用户时抛出
     */
    @Override
    public boolean delete(Long id, Long userId) {
        CartItemPo existing = cartItemManager.getById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        return cartItemManager.removeById(id);
    }

    /**
     * 统计用户拥有的全部购物车记录.
     *
     * @param userId 购物车所属用户
     * @return 记录数量，包含未勾选记录
     */
    @Override
    public long countByUser(Long userId) {
        return cartItemManager.count(
                Wrappers.lambdaQuery(CartItemPo.class).eq(CartItemPo::getUserId, userId));
    }

    /**
     * 将非空数量和勾选字段应用到指定用户拥有的购物车记录.
     *
     * @param id 购物车记录主键
     * @param userId 预期所属用户
     * @param dto 可选更新字段
     * @return 数据层更新成功时返回 true
     * @throws BizException 记录不存在或属于其他用户时抛出
     */
    @Override
    public boolean update(Long id, Long userId, CartUpdateDto dto) {
        CartItemPo existing = cartItemManager.getById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        if (dto.getQuantity() != null) {
            existing.setQuantity(dto.getQuantity());
        }
        if (dto.getIsSelected() != null) {
            existing.setSelected(dto.getIsSelected() ? 1 : 0);
        }
        return cartItemManager.updateById(existing);
    }

    /**
     * 更新用户全部购物车记录的勾选编码.
     *
     * @param userId 购物车所属用户
     * @param selected 控制器传入的持久化编码 0 或 1
     * @return 批量更新成功时返回 true，勾选状态无变化也按更新结果返回
     */
    @Override
    public boolean selectAll(Long userId, Integer selected) {
        return cartItemManager.update(
                Wrappers.lambdaUpdate(CartItemPo.class)
                        .eq(CartItemPo::getUserId, userId)
                        .set(CartItemPo::getSelected, selected));
    }

    /**
     * 删除用户拥有的全部购物车记录.
     *
     * @param userId 购物车所属用户
     * @return 批量删除成功时返回 true
     */
    @Override
    public boolean clear(Long userId) {
        return cartItemManager.remove(
                Wrappers.lambdaQuery(CartItemPo.class).eq(CartItemPo::getUserId, userId));
    }

    /**
     * 将用户拥有的购物车记录转换为收藏，再删除对应本地记录.
     *
     * <p>方法先要求每个去重后的请求主键都解析为用户拥有的记录。删除前会按 SKU 逐个调用用户服务；
     * 任一远程调用失败都会停止后续流程且不删除本地记录。该方法没有本地事务注解，因此后续步骤失败
     * 时不会回滚此前已写入的收藏。
     *
     * @param userId 购物车所属用户
     * @param itemIds 非空购物车记录主键
     * @throws BizException 列表为空、记录缺失或越权、收藏写入失败或本地删除失败时抛出
     */
    public void moveToFavorite(Long userId, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        List<CartItemPo> items =
                cartItemManager.list(
                        Wrappers.lambdaQuery(CartItemPo.class)
                                .eq(CartItemPo::getUserId, userId)
                                .in(CartItemPo::getId, itemIds));
        // 条目数与入参不匹配说明存在不属于当前用户或已删除的条目，整体失败避免部分收藏
        if (items.size() != itemIds.stream().distinct().count()) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        for (CartItemPo item : items) {
            Result<Void> result = cartRemoteGateway.addFavorite(userId, item.getSkuId());
            if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
                log.error(
                        "moveToFavorite 调用 addFavorite 失败，userId={}，skuId={}，code={}",
                        userId,
                        item.getSkuId(),
                        result == null ? null : result.getCode());
                throw new BizException(CartCodeEnum.MOVE_TO_FAVORITE_FAIL);
            }
        }
        if (!cartItemManager.removeByIds(itemIds)) {
            // 删除失败抛异常触发事务回滚，购物车条目保持原状，由调用方重试
            throw new BizException(CartCodeEnum.MOVE_TO_FAVORITE_FAIL);
        }
        log.info("购物车商品已移入收藏，userId={}，itemIds={}", userId, itemIds);
    }

    /**
     * 为请求的购物车记录构建权威结算快照.
     *
     * <p>查询前先去除重复主键，结果保持首次出现的请求顺序。每条记录都必须属于当前用户且已勾选。
     *
     * @param userId 结算用户
     * @param itemIds 请求的购物车记录主键
     * @return 购物车主键、SKU 主键和数量组成的条目
     * @throws BizException 列表为空、记录缺失或越权、条目未勾选时抛出
     */
    @Override
    public List<CartCheckoutItemDto> getCheckoutItems(Long userId, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BizException(CommonCode.PARAM_INVALID, "购物车条目不能为空");
        }
        List<Long> distinctIds = itemIds.stream().distinct().toList();
        List<CartItemPo> rows =
                cartItemManager.list(
                        Wrappers.lambdaQuery(CartItemPo.class)
                                .eq(CartItemPo::getUserId, userId)
                                .in(CartItemPo::getId, distinctIds));
        if (rows.size() != distinctIds.size()) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        Map<Long, CartItemPo> rowMap =
                rows.stream().collect(Collectors.toMap(CartItemPo::getId, Function.identity()));
        List<CartCheckoutItemDto> result = new ArrayList<>(distinctIds.size());
        for (Long itemId : distinctIds) {
            CartItemPo row = rowMap.get(itemId);
            if (row.getSelected() == null || row.getSelected() != 1) {
                throw new BizException(CartCodeEnum.CART_ITEM_NOT_SELECTED);
            }
            result.add(new CartCheckoutItemDto(row.getId(), row.getSkuId(), row.getQuantity()));
        }
        return result;
    }

    /**
     * 在一个本地事务中原子应用购物车记录的已结算数量.
     *
     * <p>数据库操作前先校验必填字段和重复主键。SKU 不一致会使整个操作失败。当前数量小于等于结算量
     * 时删除记录，否则扣减结算量。已缺失记录会跳过，以支持安全重试。
     *
     * @param userId 结算用户
     * @param items 已结算的购物车主键、SKU 主键和数量
     * @throws BizException 入参非法、SKU 不一致或本地删除、更新失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupCheckout(Long userId, List<CartCheckoutItemDto> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(CommonCode.PARAM_INVALID, "购物车清理项不能为空");
        }
        Map<Long, CartCheckoutItemDto> expectedMap = new HashMap<>(items.size() * 2);
        for (CartCheckoutItemDto item : items) {
            if (item == null
                    || item.getCartItemId() == null
                    || item.getSkuId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw new BizException(CommonCode.PARAM_INVALID, "购物车清理项非法");
            }
            if (expectedMap.putIfAbsent(item.getCartItemId(), item) != null) {
                throw new BizException(CommonCode.PARAM_INVALID, "购物车条目重复");
            }
        }
        List<CartItemPo> rows =
                cartItemManager.list(
                        Wrappers.lambdaQuery(CartItemPo.class)
                                .eq(CartItemPo::getUserId, userId)
                                .in(CartItemPo::getId, expectedMap.keySet()));
        Map<Long, CartItemPo> rowMap =
                rows.stream().collect(Collectors.toMap(CartItemPo::getId, Function.identity()));
        for (Map.Entry<Long, CartCheckoutItemDto> entry : expectedMap.entrySet()) {
            CartItemPo row = rowMap.get(entry.getKey());
            if (row != null && !row.getSkuId().equals(entry.getValue().getSkuId())) {
                throw new BizException(CartCodeEnum.CART_ITEM_MISMATCH);
            }
        }

        List<Long> removeIds = new ArrayList<>(expectedMap.size());
        List<CartItemPo> updates = new ArrayList<>(expectedMap.size());
        for (Map.Entry<Long, CartCheckoutItemDto> entry : expectedMap.entrySet()) {
            CartItemPo row = rowMap.get(entry.getKey());
            if (row == null) {
                continue;
            }
            int currentQuantity = row.getQuantity() == null ? 0 : row.getQuantity();
            if (currentQuantity <= entry.getValue().getQuantity()) {
                removeIds.add(row.getId());
                continue;
            }
            row.setQuantity(currentQuantity - entry.getValue().getQuantity());
            updates.add(row);
        }
        if (!removeIds.isEmpty() && !cartItemManager.removeByIds(removeIds)) {
            throw new BizException(CartCodeEnum.CART_CHECKOUT_CLEANUP_FAIL);
        }
        for (CartItemPo row : updates) {
            if (!cartItemManager.updateById(row)) {
                throw new BizException(CartCodeEnum.CART_CHECKOUT_CLEANUP_FAIL);
            }
        }
    }
}
