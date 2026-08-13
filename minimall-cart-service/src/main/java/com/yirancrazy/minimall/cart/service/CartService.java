package com.yirancrazy.minimall.cart.service;

import java.util.List;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
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
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @return 购物车项列表
     */
    List<CartItemPO> listByUser(Long userId);

    /**
     * 添加购物车项。
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @param dto 购物车添加DTO
     * @return 购物车项ID
     */
    Long add(Long userId, CartItemAddDTO dto);

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
     * 部分更新购物车项，仅更新非空字段（quantity / isSelected），不存在时抛出业务异常。
     * @param id 购物车项ID
     * @param dto 更新入参
     * @return 更新是否成功
     */
    boolean update(Long id, CartUpdateDTO dto);

    /**
     * 全选或取消全选指定用户的购物车项。
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @param selected 选中状态，0 或 1
     * @return 更新是否成功
     */
    boolean selectAll(Long userId, Integer selected);

    /**
     * 清空指定用户的购物车。
     * @param userId 用户ID
     * @return 清空是否成功
     */
    boolean clear(Long userId);

    /**
     * 将指定用户的某个购物车商品移入收藏夹：先调用收藏服务收藏，成功后删除该购物车项。
     * @param userId 用户ID
     * @param skuId 商品SKU ID
     */
    void moveToFavorite(Long userId, Long skuId);
}