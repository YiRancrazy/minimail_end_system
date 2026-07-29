package com.yirancrazy.minimall.cart.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartItemListDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.service.CartService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车领域服务实现，委托 CartItemManager 完成数据访问并补充默认选中状态等业务规则。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Service
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

    @Override
    public long countByUser(Long userId) {
        return cartItemManager.count(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId));
    }
}