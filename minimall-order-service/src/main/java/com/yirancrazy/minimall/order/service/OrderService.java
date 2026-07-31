package com.yirancrazy.minimall.order.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单领域服务接口，定义Order相关业务契约
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface OrderService {
    Long create(Long userId, Long skuId, Integer quantity);

    void pay(Long orderId);

    void cancel(Long orderId, Long userId);

    void ship(Long orderId, Long merchantId);

    void confirm(Long orderId, Long userId);

    void refund(Long orderId);

    void handleRefundCallback(Long orderId, boolean success);

    Integer getStatus(Long orderId);
}
