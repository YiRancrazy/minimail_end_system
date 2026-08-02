package com.yirancrazy.minimall.cart.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.cart.constant.CartCodeEnum;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartItemListDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectAllDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车领域服务实现，实现Cart相关业务逻辑
 * @Version: 1.1
 * @DateTime: 2026/08/02
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
     * 修改购物车项数量，不存在时抛出 CART_ITEM_NOT_FOUND。
     *
     * @param id 购物车项 ID
     * @param dto 数量修改入参
     * @return 更新是否成功
     */
    @Override
    public boolean updateQuantity(Long id, CartUpdateDTO dto) {
        CartItemPO existing = cartItemManager.getById(id);
        if (existing == null) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        existing.setQuantity(dto.getQuantity());
        return cartItemManager.updateById(existing);
    }

    /**
     * 修改购物车项勾选状态，不存在时抛出 CART_ITEM_NOT_FOUND。
     *
     * @param id 购物车项 ID
     * @param dto 勾选状态入参
     * @return 更新是否成功
     */
    @Override
    public boolean select(Long id, CartSelectDTO dto) {
        CartItemPO existing = cartItemManager.getById(id);
        if (existing == null) {
            throw new BizException(CartCodeEnum.CART_ITEM_NOT_FOUND);
        }
        existing.setSelected(dto.getSelected());
        return cartItemManager.updateById(existing);
    }

    /**
     * 全选或取消全选指定用户的购物车项。
     *
     * @param dto 全选入参
     * @return 更新是否成功
     */
    @Override
    public boolean selectAll(CartSelectAllDTO dto) {
        return cartItemManager.update(Wrappers.lambdaUpdate(CartItemPO.class)
            .eq(CartItemPO::getUserId, dto.getUserId())
            .set(CartItemPO::getSelected, dto.getSelected()));
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
}
