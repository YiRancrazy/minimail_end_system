package com.yirancrazy.minimall.cart.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.service.CartService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemManager cartItemManager;

    public CartServiceImpl(CartItemManager cartItemManager) {
        this.cartItemManager = cartItemManager;
    }

    @Override
    public List<CartItemPO> listByUser(Long userId) {
        return cartItemManager.list(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId));
    }

    @Override
    public Long add(CartItemPO item) {
        if (item.getSelected() == null) {
            item.setSelected(1);
        }
        cartItemManager.save(item);
        return item.getId();
    }

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