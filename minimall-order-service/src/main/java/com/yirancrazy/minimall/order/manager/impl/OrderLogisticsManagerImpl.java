package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPO;
import com.yirancrazy.minimall.order.manager.OrderLogisticsManager;
import com.yirancrazy.minimall.order.mapper.OrderLogisticsMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderLogistics 数据访问层实现，封装 t_order_logistics 表 CRUD
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Manager
public class OrderLogisticsManagerImpl extends ServiceImpl<OrderLogisticsMapper, OrderLogisticsPO>
    implements OrderLogisticsManager {
}
