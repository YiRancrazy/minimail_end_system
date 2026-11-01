package com.yirancrazy.minimall.order.service;

import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义订单事件在业务事务内写入发件箱的能力。
 *   业务边界：调用方必须处于现有事务中，以保证业务状态与待投递事件同时提交或回滚。
 *   使用场景：OrderServiceImpl 在支付状态推进事务内调用。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
public interface OrderOutboxService {

    /**
     * 在当前事务中记录订单支付成功事件.
     *
     * @param event 订单支付成功事件
     */
    void recordPaidEvent(OrderPaidDto event);
}
