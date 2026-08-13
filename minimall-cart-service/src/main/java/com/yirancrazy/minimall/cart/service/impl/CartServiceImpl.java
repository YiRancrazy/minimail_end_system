package com.yirancrazy.minimall.cart.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.cart.constant.CartCodeEnum;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车领域服务实现，实现Cart相关业务逻辑
 * @Version: 1.2
 * @DateTime: 2026/08/03
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    private final CartItemManager cartItemManager;
    private final UserFeignClient userFeignClient;

    public CartServiceImpl(CartItemManager cartItemManager, UserFeignClient userFeignClient) {
        this.cartItemManager = cartItemManager;
        this.userFeignClient = userFeignClient;
    }

    /**
     * 根据用户 ID 查询其购物车全部条目。
     *
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @return 该用户购物车条目列表
     */
    @Override
    public List<CartItemPO> listByUser(Long userId) {
        return cartItemManager.list(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId));
    }

    /**
     * 新增一条购物车条目，若未设置选中状态则默认为选中。
     *
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @param dto 购物车条目信息
     * @return 新增条目的主键 ID
     */
    @Override
    public Long add(Long userId, CartItemAddDTO dto) {
        CartItemPO item = new CartItemPO();
        item.setUserId(userId);
        item.setSkuId(dto.getSkuId());
        item.setQuantity(dto.getQuantity());
        item.setSelected(dto.getSelected() != null ? dto.getSelected() : 1);
        cartItemManager.save(item);
        return item.getId();
    }

    /**
     * 根据购物车项 ID 逻辑删除该条目。
     *
     * @param id 购物车项 ID
     * @return 是否删除成功
     */
    @Override
    public boolean delete(Long id) {
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
     * 部分更新购物车项，仅更新非空字段（quantity / isSelected），不存在时抛出 CART_ITEM_NOT_FOUND。
     *
     * @param id 购物车项 ID
     * @param dto 更新入参
     * @return 更新是否成功
     */
    @Override
    public boolean update(Long id, CartUpdateDTO dto) {
        CartItemPO existing = cartItemManager.getById(id);
        if (existing == null) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        if (dto.getQuantity() != null) {
            existing.setQuantity(dto.getQuantity());
        }
        if (dto.getIsSelected() != null) {
            existing.setSelected(dto.getIsSelected());
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
     * 将指定用户的某个购物车商品移入收藏夹：先调用收藏服务收藏，成功后删除该购物车项。
     *
     * @param userId 用户ID
     * @param skuId 商品SKU ID
     */
    @Override
    public void moveToFavorite(Long userId, Long skuId) {
        Result<Void> result = userFeignClient.addFavorite(userId, skuId);
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
            log.error("moveToFavorite feign addFavorite failed, userId={}, skuId={}, code={}",
                userId, skuId, result == null ? null : result.getCode());
            throw new BizException(CartCodeEnum.MOVE_TO_FAVORITE_FAIL);
        }
        List<CartItemPO> items = cartItemManager.list(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId)
            .eq(CartItemPO::getSkuId, skuId));
        for (CartItemPO item : items) {
            delete(item.getId());
        }
        log.info("cart item moved to favorite, userId={}, skuId={}, removed={}", userId, skuId, items.size());
    }
}
