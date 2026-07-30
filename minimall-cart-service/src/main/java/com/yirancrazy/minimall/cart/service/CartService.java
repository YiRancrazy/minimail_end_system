package com.yirancrazy.minimall.cart.service;

import java.util.List;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartItemListDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
* 购物车领域服务接口，定义按用户列出、计数、添加与删除等业务能力。
 */
public interface CartService {
    List<CartItemPO> listByUser(CartItemListDTO dto);

    Long add(CartItemAddDTO dto);

    boolean delete(Long id);

    long countByUser(Long userId);
}