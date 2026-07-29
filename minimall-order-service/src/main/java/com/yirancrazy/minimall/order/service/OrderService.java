package com.yirancrazy.minimall.order.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单领域服务接口，定义订单创建、支付状态推进及订单状态查询的业务契约。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface OrderService {
    Long create(Long userId, Long skuId, Integer quantity);

    boolean pay(Long orderId);

    String status(Long orderId);
}