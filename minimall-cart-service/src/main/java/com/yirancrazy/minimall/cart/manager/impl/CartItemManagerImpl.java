package com.yirancrazy.minimall.cart.manager.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.mapper.CartItemMapper;
import com.yirancrazy.minimall.common.annotation.Manager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItem数据访问层实现，封装CartItem表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Manager
public class CartItemManagerImpl extends ServiceImpl<CartItemMapper, CartItemPO>
    implements CartItemManager {

    /**
     * 根据用户ID和SKU ID查询购物车条目。
     * @param userId 用户ID，来自网关X-User-Id可信头
     * @param skuId SKU ID
     * @return 购物车条目；不存在返回 null
     */
    @Override
    public CartItemPO getByUserIdAndSkuId(Long userId, Long skuId) {
        return getOne(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId)
            .eq(CartItemPO::getSkuId, skuId));
    }
}