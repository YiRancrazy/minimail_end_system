package com.yirancrazy.minimall.order.service;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单发件箱发布器，负责将待发送事件投递到消息队列。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
public interface OrderOutboxPublisher {

    /**
     * 发布待处理发件箱事件。
     *
     * @param limit 单批处理数量
     * @return 成功发布的事件数量
     */
    int publishPending(int limit);
}
