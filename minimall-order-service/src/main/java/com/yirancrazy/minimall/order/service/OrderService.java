package com.yirancrazy.minimall.order.service;

public interface OrderService {
    Long create(Long userId, Long skuId, Integer quantity);

    boolean pay(Long orderId);

    String status(Long orderId);
}