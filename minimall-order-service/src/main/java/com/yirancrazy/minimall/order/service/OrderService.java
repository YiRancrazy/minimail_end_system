package com.yirancrazy.minimall.order.service;

/**
* 订单领域服务接口，定义订单创建、支付状态推进及订单状态查询的业务契约。
 */
public interface OrderService {
    /**
     * 创建订单。
     * @param userId 用户ID
     * @param skuId 商品SKU ID
     * @param quantity 购买数量
     * @return 订单ID
     */
    Long create(Long userId, Long skuId, Integer quantity);

    /**
     * 订单支付。
     * @param orderId 订单ID
     * @return 支付是否成功
     */
    boolean pay(Long orderId);

    /**
     * 查询订单状态。
     * @param orderId 订单ID
     * @return 订单状态
     */
    String status(Long orderId);
}