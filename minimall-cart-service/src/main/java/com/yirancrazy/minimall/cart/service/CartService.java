package com.yirancrazy.minimall.cart.service;

import java.util.List;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartItemListDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartService description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface CartService {
    /**
     * 根据用户查询购物车列表。
     * @param dto 购物车列表查询DTO
     * @return 购物车项列表
     */
    List<CartItemPO> listByUser(CartItemListDTO dto);

    /**
     * 添加购物车项。
     * @param dto 购物车添加DTO
     * @return 购物车项ID
     */
    Long add(CartItemAddDTO dto);

    /**
     * 删除购物车项。
     * @param id 购物车项ID
     * @return 删除是否成功
     */
    boolean delete(Long id);

    /**
     * 统计用户购物车项数量。
     * @param userId 用户ID
     * @return 购物车项数量
     */
    long countByUser(Long userId);
}