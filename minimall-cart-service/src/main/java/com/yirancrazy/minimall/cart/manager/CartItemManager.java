package com.yirancrazy.minimall.cart.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车项数据访问管理接口，继承 MyBatis-Plus IService 提供通用 CRUD 能力。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface CartItemManager extends IService<CartItemPO> {
}