package com.yirancrazy.minimall.notify.listener;

import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;
import com.yirancrazy.minimall.common.event.RocketMqEventConsumer;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.service.NotifyService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在 RocketMQ 事件总线启用时消费订单支付事件，以 Redis 订单键和数据库业务唯一键双重防重，并在通知落库失败时抛出异常触发重投.
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    /**
     * 保存通知服务、Redis、RocketMQ 地址、主题和消费组配置；构造阶段不启动消费者.
     *
     * @param notifyService 站内通知写入服务
     * @param redis 订单支付事件去重键存储
     * @param namesrv RocketMQ NameServer 地址
     * @param topic 订单支付事件所在主题
     * @param group 通知服务消费组
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

    /** 创建并启动订单支付事件消费者，订阅后可接收 OrderPaidDto 并回调 onPaid. */
    @PostConstruct
    void init() {
        consumer = new RocketMqEventConsumer(namesrv, topic, group);
        consumer.subscribe(OrderPaidDto.class, this::onPaid);
        consumer.start();
        log.info("OrderPaidMqConsumer 已就绪（namesrv={}，topic={}）", namesrv, topic);
    }

    /** 应用关闭前停止 RocketMQ 消费者；消费者尚未创建时不做处理. */
    @PreDestroy
    void close() {
        if (consumer != null) {
            consumer.stop();
        }
    }

    /**
     * 处理订单支付事件：空事件直接忽略，Redis 已存在则跳过；否则写入业务唯一通知，成功后设置 7 天去重键，落库失败抛出业务异常.
     *
     * @param event 订单支付事件；订单编号为空时不产生任何副作用
     * @throws BizException 通知保存返回 false 时抛出，使消息可被重新投递
     */
    void onPaid(OrderPaidDto event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        String key = "notify:order:paid:" + event.getOrderId();
        // 防止 Redis 读取失败，导致业务逻辑错误
        String redisKey = null;
        try {
            redisKey = redis.opsForValue().get(key);
        } catch (Exception e) {
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
            } catch (Exception e) {
                log.warn("Redis 写入失败, 已忽略, key={}", key, e);
            }
            log.info("已通过 mq 通知用户 {} 订单 {} 支付成功", event.getUserId(), event.getOrderId());
        } else {
            throw new BizException(NotifyCodeEnum.NOTIFY_SAVE_FAILED);
        }
    }
}
