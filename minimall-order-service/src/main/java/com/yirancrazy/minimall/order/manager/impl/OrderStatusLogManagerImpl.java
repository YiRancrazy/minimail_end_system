package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OrderStatusLogPo;
import com.yirancrazy.minimall.order.manager.OrderStatusLogManager;
import com.yirancrazy.minimall.order.mapper.OrderStatusLogMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现 OrderStatusLogManager，通过 MyBatis-Plus 访问OrderStatusLogPo。
 *   业务边界：作为 @Manager Bean 只处理持久化细节，不承载跨服务事务和业务状态机。
 *   使用场景：由订单服务或补偿任务依赖 base 接口调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class OrderStatusLogManagerImpl extends ServiceImpl<OrderStatusLogMapper, OrderStatusLogPo>
    implements OrderStatusLogManager {}
