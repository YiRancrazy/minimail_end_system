package com.yirancrazy.minimall.notify.listener;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.common.event.RocketMqEventConsumer;
import com.yirancrazy.minimall.notify.service.NotifyService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaidMq消费者，消费OrderPaidMq相关消息并落库站内信
 * @Version: 1.2
 * @DateTime: 2026/08/03
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "minimall.eventbus.rocketmq.enabled", havingValue = "true")
public class OrderPaidMqConsumer {

    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final NotifyService notifyService;
    private final StringRedisTemplate redis;
    private final String namesrv;
    private final String topic;
    private final String group;
    private RocketMqEventConsumer consumer;

    public OrderPaidMqConsumer(
        NotifyService notifyService,
        StringRedisTemplate redis,
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrv,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic,
        @Value("${minimall.eventbus.rocketmq.group:minimall-notify}") String group) {
        this.notifyService = notifyService;
        this.redis = redis;
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
        if (consumer != null) {
            consumer.stop();
        }
    }

    /**
     * 消费订单支付事件，Redis SETNX 幂等去重后落库站内信；SSE 推送由 push 内部统一负责。
     * @param event 订单支付事件
     */
    void onPaid(OrderPaidDTO event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        String key = "notify:order:paid:" + event.getOrderId();
        Boolean first = redis.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
        if (Boolean.FALSE.equals(first)) {
            log.info("duplicate order paid event skipped, orderId={}", event.getOrderId());
            return;
        }
        try {
            String title = "订单支付成功";
            String content = "订单 " + event.getOrderId() + " 已支付，金额 " + event.getAmount();
            notifyService.push(event.getUserId(), title, content);
            log.info("notified user {} of order {} via mq", event.getUserId(), event.getOrderId());
        }
        catch (Exception e) {
            // DB 落库失败时删除幂等键：否则 RocketMQ 重投会被 SETNX 挡掉，支付成功通知永久丢失
            redis.delete(key);
            throw e;
        }
    }
}
