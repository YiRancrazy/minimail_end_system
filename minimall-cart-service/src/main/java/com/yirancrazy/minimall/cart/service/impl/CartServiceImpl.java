package com.yirancrazy.minimall.cart.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.cart.constant.CartCodeEnum;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.cart.vo.CartItemVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车领域服务实现，实现Cart相关业务逻辑
 * @Version: 1.4
 * @DateTime: 2026/08/03
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    // 购物车单项数量上限，与 CartItemAddDTO/CartUpdateDTO 的 @Max(999) 校验保持一致
    private static final int MAX_QUANTITY = 999;

    private final CartItemManager cartItemManager;
    private final UserFeignClient userFeignClient;
    private final GoodsFeignClient goodsFeignClient;
    private final MinioUtil minioUtil;

    public CartServiceImpl(CartItemManager cartItemManager, UserFeignClient userFeignClient,
                           GoodsFeignClient goodsFeignClient, MinioUtil minioUtil) {
        this.cartItemManager = cartItemManager;
        this.userFeignClient = userFeignClient;
        this.goodsFeignClient = goodsFeignClient;
        this.minioUtil = minioUtil;
    }

    /**
     * 根据用户 ID 查询其购物车全部条目，并叠加 SKU/SPU 快照用于前端渲染。
     *
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @return 该用户购物车条目列表
     */
    @Override
    public List<CartItemVO> listByUser(Long userId) {
        List<CartItemPO> items = cartItemManager.list(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId));
        if (items.isEmpty()) {
            return List.of();
        }
        // 批量快照替代逐条目 2N 次 Feign：1 次批量 SKU + 按需 1 次批量 SPU；缺失快照沿用原降级逻辑
        List<Long> skuIds = items.stream().map(CartItemPO::getSkuId).filter(Objects::nonNull).toList();
        Map<Long, SkuSnapshotDTO> skuMap = goodsFeignClient.batchSkuSnapshot(skuIds).getData();
        List<Long> spuIds = skuMap == null ? List.of() : skuMap.values().stream()
            .map(SkuSnapshotDTO::getSpuId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SpuSnapshotDTO> spuMap = spuIds.isEmpty() ? Map.of()
            : goodsFeignClient.batchSpuSnapshot(spuIds).getData();
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
     * 新增一条购物车条目；同用户同 SKU 已存在时累加数量（上限 999），否则新建。
     * SKU 有效性未在此校验，由结算链路兜底（快照缺失时购物车展示降级、下单时校验）。
     *
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @param dto 购物车条目信息
     * @return 购物车条目 ID（合并时返回已有条目 ID）
     */
    @Override
    public Long add(Long userId, CartItemAddDTO dto) {
        // 依赖 t_cart_item 的 uk_user_sku 唯一索引，同 SKU 只能存在一行，重复加入时累加数量
        CartItemPO existing = cartItemManager.getByUserIdAndSkuId(userId, dto.getSkuId());

        // 数据存在则对应数量+1
        if (existing != null) {
            int merged = existing.getQuantity() + dto.getQuantity();
            existing.setQuantity(Math.min(merged, MAX_QUANTITY));
            cartItemManager.updateById(existing);
            return existing.getId();
        }

        // 数据不存在则新建
        CartItemPO item = new CartItemPO();
        item.setUserId(userId);
        item.setSkuId(dto.getSkuId());
        item.setQuantity(dto.getQuantity());
        item.setSelected(dto.getSelected() != null ? dto.getSelected() : 1);
        cartItemManager.save(item);
        return item.getId();
    }

    /**
     * 根据购物车项 ID 逻辑删除该条目，仅允许删除归属于当前用户的条目。
     *
     * @param id 购物车项 ID
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @return 是否删除成功
     * @throws BizException 条目不存在或不属于该用户时抛 CART_ITEM_NOT_FOUND
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
     * 统计用户购物车项数量。
     * @param userId 用户ID
     * @return 购物车项数量
     */
    @Override
    public long countByUser(Long userId) {
        return cartItemManager.count(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId));
    }

    /**
     * 部分更新购物车项，仅更新非空字段（quantity / isSelected），条目不存在或不属于当前用户时抛出 CART_ITEM_NOT_FOUND。
     *
     * @param id 购物车项 ID
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @param dto 更新入参
     * @return 更新是否成功
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
     * 全选或取消全选指定用户的购物车项。
     *
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @param selected 选中状态，0 或 1
     * @return 更新是否成功
     */
    @Override
    public boolean selectAll(Long userId, Integer selected) {
        return cartItemManager.update(Wrappers.lambdaUpdate(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId)
            .set(CartItemPO::getSelected, selected));
    }

    /**
     * 清空指定用户的购物车。
     *
     * @param userId 用户ID
     * @return 清空是否成功
     */
    @Override
    public boolean clear(Long userId) {
        return cartItemManager.remove(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId));
    }

    /**
     * 将指定购物车条目移入收藏夹：先调用收藏服务按 SKU 收藏，成功后按条目 ID 删除购物车项。
     * 收藏与删除在同一事务内，删除失败整体回滚，避免"已收藏但条目残留"的重复收藏状态；
     * 按条目 ID 精确删除，避免按 userId+skuId 批量删误伤并发加购的同 SKU 新条目。
     *
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @param itemIds 待移入收藏夹的购物车条目 ID 列表，不可为空且必须全部归属于该用户
     * @throws BizException 条目不存在/不属于该用户时抛 CART_ITEM_NOT_FOUND；收藏或删除失败时抛 MOVE_TO_FAVORITE_FAIL
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
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
            Result<Void> result = userFeignClient.addFavorite(userId, item.getSkuId());
            if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
                log.error("moveToFavorite feign addFavorite failed, userId={}, skuId={}, code={}",
                    userId, item.getSkuId(), result == null ? null : result.getCode());
                throw new BizException(CartCodeEnum.MOVE_TO_FAVORITE_FAIL);
            }
        }
        if (!cartItemManager.removeByIds(itemIds)) {
            // 删除失败抛异常触发事务回滚，购物车条目保持原状，由调用方重试
            throw new BizException(CartCodeEnum.MOVE_TO_FAVORITE_FAIL);
        }
        log.info("cart items moved to favorite, userId={}, itemIds={}", userId, itemIds);
    }
}
