package com.yirancrazy.minimall.cart.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.mapper.CartItemMapper;
import com.yirancrazy.minimall.common.annotation.Manager;
import org.springframework.stereotype.Service;

/**
* 购物车项数据访问管理实现，基于 MyBatis-Plus ServiceImpl 复用通用 CRUD。
 */
@Service
@Manager
public class CartItemManagerImpl extends ServiceImpl<CartItemMapper, CartItemPO>
    implements CartItemManager {
}