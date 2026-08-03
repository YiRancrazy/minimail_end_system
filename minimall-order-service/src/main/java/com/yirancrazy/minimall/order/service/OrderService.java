package com.yirancrazy.minimall.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单领域服务接口，定义Order相关业务契约
 * @Version: 1.1
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

    /**
     * 分页查询订单，支持按用户/商家/状态过滤。
     * @param dto 分页查询入参
     * @return 订单分页结果
     */
    IPage<OrderPO> page(OrderPageDTO dto);
}
