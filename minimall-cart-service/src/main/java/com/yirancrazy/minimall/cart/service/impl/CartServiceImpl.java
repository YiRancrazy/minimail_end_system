package com.yirancrazy.minimall.cart.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartItemListDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.service.CartService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车领域服务实现，实现Cart相关业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class CartServiceImpl implements CartService {

    private final CartItemManager cartItemManager;

    public CartServiceImpl(CartItemManager cartItemManager) {
        this.cartItemManager = cartItemManager;
    }

    /**
     * 根据用户 ID 查询其购物车全部条目。
     *
     * @param dto 查询条件
     * @return 该用户购物车条目列表
     */
    @Override
    public List<CartItemPO> listByUser(CartItemListDTO dto) {
        return cartItemManager.list(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, dto.getUserId()));
    }

    /**
     * 新增一条购物车条目，若未设置选中状态则默认为选中。
     *
     * @param dto 购物车条目信息
     * @return 新增条目的主键 ID
     */
    @Override
    public Long add(CartItemAddDTO dto) {
        CartItemPO item = new CartItemPO();
        item.setUserId(dto.getUserId());
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
}