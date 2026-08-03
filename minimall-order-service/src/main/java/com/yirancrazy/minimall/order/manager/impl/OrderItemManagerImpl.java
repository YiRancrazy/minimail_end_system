package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OrderItemPO;
import com.yirancrazy.minimall.order.manager.OrderItemManager;
import com.yirancrazy.minimall.order.mapper.OrderItemMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderItem数据访问层实现，封装OrderItem表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Manager
public class OrderItemManagerImpl extends ServiceImpl<OrderItemMapper, OrderItemPO>
    implements OrderItemManager {
}
