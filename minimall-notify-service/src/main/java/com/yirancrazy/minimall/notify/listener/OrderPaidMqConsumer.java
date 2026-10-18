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
 * @Description: OrderPaidMqConsumer 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
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

    /**
     * 构造 OrderPaidMqConsumer 实例。
     * @param notifyService notifyService 参数
     * @param redis redis 参数
     * @param namesrv namesrv 参数
     * @param topic topic 参数
     * @param group group 参数
     */
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

    /**
     * 初始化init。
     */
    @PostConstruct
    void init() {
        consumer = new RocketMqEventConsumer(namesrv, topic, group);
        consumer.subscribe(OrderPaidDTO.class, this::onPaid);
        consumer.start();
        log.info("OrderPaidMqConsumer 已就绪（namesrv={}，topic={}）", namesrv, topic);
    }

    /**
     * 释放当前资源。
     */
    @PreDestroy
    void close() {
        if (consumer != null) {
            consumer.stop();
        }
    }


    /**
     * 处理Paid。
     * @param event 事件
     */
    void onPaid(OrderPaidDTO event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        String key = "notify:order:paid:" + event.getOrderId();
        Boolean first = redis.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
        if (Boolean.FALSE.equals(first)) {
            log.info("重复订单支付事件已跳过，orderId={}", event.getOrderId());
            return;
        }
        try {
            String title = "订单支付成功";
            String content = "订单 " + event.getOrderId() + " 已支付，金额 " + event.getAmount();
            notifyService.push(event.getUserId(), title, content);
            log.info("已通过 mq 通知用户 {} 订单 {} 支付成功", event.getUserId(), event.getOrderId());
        }
        catch (Exception e) {
            // DB 落库失败时删除幂等键：否则 RocketMQ 重投会被 SETNX 挡掉，支付成功通知永久丢失
            redis.delete(key);
            throw e;
        }
    }
}
