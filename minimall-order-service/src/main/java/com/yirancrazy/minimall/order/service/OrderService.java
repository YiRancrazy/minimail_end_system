package com.yirancrazy.minimall.order.service;

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
