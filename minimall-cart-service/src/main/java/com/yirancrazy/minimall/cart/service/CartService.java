package com.yirancrazy.minimall.cart.service;

import com.yirancrazy.minimall.cart.entity.CartItemPO;

import java.util.List;

public interface CartService {
    List<CartItemPO> listByUser(Long userId);

    Long add(CartItemPO item);

    boolean delete(Long id);

    long countByUser(Long userId);
}