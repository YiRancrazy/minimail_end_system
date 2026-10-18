package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OrderStatusLogPO;
import com.yirancrazy.minimall.order.manager.OrderStatusLogManager;
import com.yirancrazy.minimall.order.mapper.OrderStatusLogMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderStatusLogManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class OrderStatusLogManagerImpl extends ServiceImpl<OrderStatusLogMapper, OrderStatusLogPO>
    implements OrderStatusLogManager {
}
