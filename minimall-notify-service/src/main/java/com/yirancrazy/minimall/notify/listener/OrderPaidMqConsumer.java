package com.yirancrazy.minimall.notify.listener;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.common.event.RocketMqEventConsumer;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.sse.SseHub;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaidMq消费者，消费OrderPaidMq相关消息
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class OrderPaidMqConsumer {

    private final NotifyService notifyService;
    private final SseHub sseHub;
    private final String namesrv;
    private final String topic;
    private final String group;
    private RocketMqEventConsumer consumer;

    public OrderPaidMqConsumer(
        NotifyService notifyService,
        SseHub sseHub,
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrv,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic,
        @Value("${minimall.eventbus.rocketmq.group:minimall-notify}") String group) {
        this.notifyService = notifyService;
        this.sseHub = sseHub;
        this.namesrv = namesrv;
        this.topic = topic;
        this.group = group;
    }

    @PostConstruct
    void init() {
        consumer = new RocketMqEventConsumer(namesrv, topic, group);
        consumer.subscribe(OrderPaidDTO.class, this::onPaid);
        consumer.start();
        log.info("OrderPaidMqConsumer ready (namesrv={}, topic={})", namesrv, topic);
    }

    @PreDestroy
    void close() {
        if (consumer != null) consumer.stop();
    }

    /** Package-private for direct invocation from unit tests. */
    void onPaid(OrderPaidDTO event) {
        if (event == null || event.getOrderId() == null) return;
        String title = "订单支付成功";
        String content = "订单 " + event.getOrderId() + " 已支付，金额 " + event.getAmount();
        notifyService.push(event.getUserId(), title, content);
        sseHub.send(event.getUserId(), title + ": " + content);
        log.info("notified user {} of order {} via mq", event.getUserId(), event.getOrderId());
    }
}