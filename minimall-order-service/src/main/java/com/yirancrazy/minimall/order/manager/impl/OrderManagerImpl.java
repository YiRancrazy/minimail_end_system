package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.mapper.OrderMapper;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单数据访问管理实现，基于 MyBatis-Plus 与 OrderMapper 完成 OrderPO 的持久化操作。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Service
@Manager
public class OrderManagerImpl extends ServiceImpl<OrderMapper, OrderPO>
    implements OrderManager {
}