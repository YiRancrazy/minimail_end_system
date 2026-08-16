package com.yirancrazy.minimall.notify.listener;

import java.time.Duration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaid监听器，监听OrderPaid相关事件并落库站内信
 * @Version: 1.2
 * @DateTime: 2026/08/03
 */
@Slf4j
@Component
public class OrderPaidListener {

    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final NotifyService notifyService;
    private final StringRedisTemplate redis;

    public OrderPaidListener(NotifyService notifyService, StringRedisTemplate redis) {
        this.notifyService = notifyService;
        this.redis = redis;
    }

    /**
     * 订阅 in-process LocalEventBus（订单服务在同一进程 publish 时才会触发）。
     * 双通道去重：与 OrderPaidMqConsumer 共享同一 SETNX 幂等键 notify:order:paid:{orderId}，
     * 本地事件与 MQ 同时到达时仅第一个通道落库，第二个被 SETNX 跳过；落库失败删除幂等键允许重投。
     */
    @EventListener
    public void onOrderPaid(OrderPaidDTO event) {
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
            log.info("notified user {} of order {}", event.getUserId(), event.getOrderId());
        }
        catch (Exception e) {
            // DB 落库失败时删除幂等键：否则事件重投会被 SETNX 挡掉，支付成功通知永久丢失
            redis.delete(key);
            throw e;
        }
    }
}
