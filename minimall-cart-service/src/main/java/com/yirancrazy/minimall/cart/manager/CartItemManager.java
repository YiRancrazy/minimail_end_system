package com.yirancrazy.minimall.cart.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
* 购物车项数据访问管理接口，继承 MyBatis-Plus IService 提供通用 CRUD 能力。
 */
public interface CartItemManager extends IService<CartItemPO> {
}