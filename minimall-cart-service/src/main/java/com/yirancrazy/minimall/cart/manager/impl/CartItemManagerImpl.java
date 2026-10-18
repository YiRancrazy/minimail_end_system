package com.yirancrazy.minimall.cart.manager.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.mapper.CartItemMapper;
import com.yirancrazy.minimall.common.annotation.Manager;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItemManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class CartItemManagerImpl extends ServiceImpl<CartItemMapper, CartItemPO>
    implements CartItemManager {


    /**
     * 获取相关数据。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @return 处理结果
     */
    @Override
    public CartItemPO getByUserIdAndSkuId(Long userId, Long skuId) {
        return getOne(Wrappers.lambdaQuery(CartItemPO.class)
            .eq(CartItemPO::getUserId, userId)
            .eq(CartItemPO::getSkuId, skuId));
    }

    /**
     * 执行 upsertQuantity 方法。
     * @param id id 参数
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param selected selected 参数
     * @return 业务数值
     */
    @Override
    public int upsertQuantity(Long id, Long userId, Long skuId, Integer quantity, Integer selected) {
        return baseMapper.upsertQuantity(id, userId, skuId, quantity, selected);
    }
}
