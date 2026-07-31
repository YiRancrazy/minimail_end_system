package com.yirancrazy.minimall.cart.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.mapper.CartItemMapper;
import com.yirancrazy.minimall.common.annotation.Manager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItemManagerImpl description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class CartItemManagerImpl extends ServiceImpl<CartItemMapper, CartItemPO>
    implements CartItemManager {
}