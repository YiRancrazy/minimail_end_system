package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.CartCleanupTaskPO;
import com.yirancrazy.minimall.order.manager.CartCleanupTaskManager;
import com.yirancrazy.minimall.order.mapper.CartCleanupTaskMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车清理任务管理器实现。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Manager
public class CartCleanupTaskManagerImpl
        extends ServiceImpl<CartCleanupTaskMapper, CartCleanupTaskPO>
        implements CartCleanupTaskManager {
}
