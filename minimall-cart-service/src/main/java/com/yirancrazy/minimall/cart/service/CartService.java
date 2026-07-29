package com.yirancrazy.minimall.cart.service;

import com.yirancrazy.minimall.cart.entity.CartItemPO;

import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车领域服务接口，定义按用户列出、计数、添加与删除等业务能力。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface CartService {
    List<CartItemPO> listByUser(Long userId);

    Long add(CartItemPO item);

    boolean delete(Long id);

    long countByUser(Long userId);
}