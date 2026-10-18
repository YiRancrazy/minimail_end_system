package com.yirancrazy.minimall.cart.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.cart.constant.CartCodeEnum;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.gateway.CartRemoteGateway;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.cart.vo.CartItemVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final CartItemManager cartItemManager;
    private final CartRemoteGateway cartRemoteGateway;
    private final MinioUtil minioUtil;

    /**
     * 构造 CartServiceImpl 实例。
     * @param cartItemManager cartItemManager 参数
     * @param cartRemoteGateway cartRemoteGateway 参数
     * @param minioUtil minioUtil 参数
     */
    public CartServiceImpl(CartItemManager cartItemManager, CartRemoteGateway cartRemoteGateway,
                           MinioUtil minioUtil) {
        this.cartItemManager = cartItemManager;
        this.cartRemoteGateway = cartRemoteGateway;
        this.minioUtil = minioUtil;
    }


    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @return 结果列表
     */
    @Override
    public List<CartItemVO> listByUser(Long userId) {
        List<CartItemPO> items = cartItemManager.list(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId));
        if (items.isEmpty()) {
            return List.of();
        }
        // 批量快照替代逐条目 2N 次远程调用：1 次批量 SKU + 按需 1 次批量 SPU；缺失快照沿用原降级逻辑
        List<Long> skuIds = items.stream().map(CartItemPO::getSkuId).filter(Objects::nonNull).toList();
        Map<Long, SkuSnapshotDTO> skuMap = cartRemoteGateway.batchSkuSnapshot(skuIds).getData();
        List<Long> spuIds = skuMap == null ? List.of() : skuMap.values().stream()
            .map(SkuSnapshotDTO::getSpuId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SpuSnapshotDTO> spuMap = spuIds.isEmpty() ? Map.of()
            : cartRemoteGateway.batchSpuSnapshot(spuIds).getData();
        Map<Long, SkuSnapshotDTO> effectiveSkuMap = skuMap == null ? Map.of() : skuMap;
        Map<Long, SpuSnapshotDTO> effectiveSpuMap = spuMap == null ? Map.of() : spuMap;
        return items.stream().map(item -> {
            SkuSnapshotDTO sku = effectiveSkuMap.get(item.getSkuId());
            SpuSnapshotDTO spu = sku == null || sku.getSpuId() == null
                ? null
                : effectiveSpuMap.get(sku.getSpuId());
            return CartItemVO.from(item, sku, spu, minioUtil);
        }).toList();
    }


    /**
     * 执行 add 方法。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    @Override
    public Long add(Long userId, CartItemAddDTO dto) {
        int selected = dto.getSelected() != null ? dto.getSelected() : 1;
        // 唯一键冲突由数据库原子累加，避免 read-check-write 丢增量或首次并发插入异常
        int affected = cartItemManager.upsertQuantity(
            IdWorker.getId(), userId, dto.getSkuId(), dto.getQuantity(), selected);
        if (affected <= 0) {
            log.error("购物车商品 upsert 未影响任何行，userId={}，skuId={}", userId, dto.getSkuId());
            throw new BizException(CommonCode.SYS_ERROR, "购物车更新失败");
        }
        CartItemPO item = cartItemManager.getByUserIdAndSkuId(userId, dto.getSkuId());
        if (item == null) {
            log.error("购物车商品 upsert 成功但无法读取记录，userId={}，skuId={}",
                userId, dto.getSkuId());
            throw new BizException(CommonCode.SYS_ERROR, "购物车更新失败");
        }
        return item.getId();
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param userId userId 参数
     * @return 是否处理成功
     */
    @Override
    public boolean delete(Long id, Long userId) {
        CartItemPO existing = cartItemManager.getById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        return cartItemManager.removeById(id);
    }


    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 业务数值
     */
    @Override
    public long countByUser(Long userId) {
        return cartItemManager.count(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId));
    }


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    @Override
    public boolean update(Long id, Long userId, CartUpdateDTO dto) {
        CartItemPO existing = cartItemManager.getById(id);
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
     * 执行 selectAll 方法。
     * @param userId userId 参数
     * @param selected selected 参数
     * @return 是否处理成功
     */
    @Override
    public boolean selectAll(Long userId, Integer selected) {
        return cartItemManager.update(Wrappers.lambdaUpdate(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId)
            .set(CartItemPO::getSelected, selected));
    }


    /**
     * 执行 clear 方法。
     * @param userId userId 参数
     * @return 是否处理成功
     */
    @Override
    public boolean clear(Long userId) {
        return cartItemManager.remove(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId));
    }


    /**
     * 执行 moveToFavorite 方法。
     * @param userId userId 参数
     * @param itemIds itemIds 参数
     */
    public void moveToFavorite(Long userId, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        List<CartItemPO> items = cartItemManager.list(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId)
            .in(CartItemPO::getId, itemIds));
        // 条目数与入参不匹配说明存在不属于当前用户或已删除的条目，整体失败避免部分收藏
        if (items.size() != itemIds.stream().distinct().count()) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        for (CartItemPO item : items) {
            Result<Void> result = cartRemoteGateway.addFavorite(userId, item.getSkuId());
            if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
                log.error("moveToFavorite 调用 addFavorite 失败，userId={}，skuId={}，code={}",
                    userId, item.getSkuId(), result == null ? null : result.getCode());
                throw new BizException(CartCodeEnum.MOVE_TO_FAVORITE_FAIL);
            }
        }
        if (!cartItemManager.removeByIds(itemIds)) {
            // 删除失败抛异常触发事务回滚，购物车条目保持原状，由调用方重试
            throw new BizException(CartCodeEnum.MOVE_TO_FAVORITE_FAIL);
        }
        log.info("购物车商品已移入收藏，userId={}，itemIds={}", userId, itemIds);
    }
}
