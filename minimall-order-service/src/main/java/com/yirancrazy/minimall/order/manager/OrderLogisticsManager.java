package com.yirancrazy.minimall.order.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装 OrderLogisticsPo 的持久化访问接口，用于订单物流节点。
 *   业务边界：继承 IService 提供基础 CRUD 和条件更新，不包含远程调用或跨聚合业务编排。
 *   使用场景：由订单服务及补偿任务调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface OrderLogisticsManager extends IService<OrderLogisticsPo> {}
