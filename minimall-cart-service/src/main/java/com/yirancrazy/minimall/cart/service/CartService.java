package com.yirancrazy.minimall.cart.service;

import java.util.List;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartItemListDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectAllDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车领域服务接口，定义Cart相关业务契约
 * @Version: 1.1
 * @DateTime: 2026/08/02
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

    /**
     * 修改购物车项数量，不存在时抛出业务异常。
     * @param id 购物车项ID
     * @param dto 数量修改入参
     * @return 更新是否成功
     */
    boolean updateQuantity(Long id, CartUpdateDTO dto);

    /**
     * 修改购物车项勾选状态，不存在时抛出业务异常。
     * @param id 购物车项ID
     * @param dto 勾选状态入参
     * @return 更新是否成功
     */
    boolean select(Long id, CartSelectDTO dto);

    /**
     * 全选或取消全选指定用户的购物车项。
     * @param dto 全选入参
     * @return 更新是否成功
     */
    boolean selectAll(CartSelectAllDTO dto);

    /**
     * 清空指定用户的购物车。
     * @param userId 用户ID
     * @return 清空是否成功
     */
    boolean clear(Long userId);
}