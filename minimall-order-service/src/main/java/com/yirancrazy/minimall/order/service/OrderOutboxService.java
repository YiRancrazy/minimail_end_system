package com.yirancrazy.minimall.order.service;

import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单发件箱服务，负责在业务事务内持久化待发送事件。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
public interface OrderOutboxService {

    /**
     * 在当前事务中记录订单支付成功事件。
     *
     * @param event 订单支付成功事件
     */
    void recordPaidEvent(OrderPaidDTO event);
}
