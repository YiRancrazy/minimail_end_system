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
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.service.NotifyService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单支付监听器
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
     * 判断订单支付事件是否被消费过，如果未消费则进行消费，否则忽略。
     * @param event 订单支付事件
     */
    void onPaid(OrderPaidDTO event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        String key = "notify:order:paid:" + event.getOrderId();
        // 防止 Redis 读取失败，导致业务逻辑错误
        String redisKey = null;
        try {
            redisKey = redis.opsForValue().get(key);
        }
        catch (Exception e) {
            log.warn("Redis 读取失败, 降级到数据库唯一键兜底, key={}", key, e);
        }

        // 如果 redisKey 不为空，说明之前已经处理过这个订单支付事件
        if (redisKey != null) {
            return;
        }

        String title = "订单支付成功";
        String content = "订单 " + event.getOrderId() + " 已支付，金额 " + event.getAmount();
        String bizId = "ORDER_PAID:" + event.getOrderId();
        boolean pushResult = notifyService.push(event.getUserId(), title, content, bizId);

        // 如果 pushResult 为 true，说明通知成功，可以设置幂等键
        if (pushResult) {
            try {
                redis.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
            }
            catch (Exception e) {
                log.warn("Redis 写入失败, 已忽略, key={}", key, e);
            }
            log.info("已通过 mq 通知用户 {} 订单 {} 支付成功", event.getUserId(), event.getOrderId());
        }
        else {
            throw new BizException(NotifyCodeEnum.NOTIFY_SAVE_FAILED);
        }
    }
}
