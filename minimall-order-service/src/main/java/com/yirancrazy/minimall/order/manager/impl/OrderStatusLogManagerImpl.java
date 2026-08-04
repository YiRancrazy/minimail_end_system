package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OrderStatusLogPO;
import com.yirancrazy.minimall.order.manager.OrderStatusLogManager;
import com.yirancrazy.minimall.order.mapper.OrderStatusLogMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderStatusLog数据访问层实现，封装订单状态日志表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Manager
public class OrderStatusLogManagerImpl extends ServiceImpl<OrderStatusLogMapper, OrderStatusLogPO>
    implements OrderStatusLogManager {
}
