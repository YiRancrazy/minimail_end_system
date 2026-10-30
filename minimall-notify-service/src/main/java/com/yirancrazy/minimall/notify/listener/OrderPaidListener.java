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
 * @Description: OrderPaidListener 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class OrderPaidListener {

    private static final Duration IDEMPOTENT_TTL = Duration.ofDays(7);

    private final NotifyService notifyService;
    private final StringRedisTemplate redis;

    /**
     * 构造 OrderPaidListener 实例。
     * @param notifyService notifyService 参数
     * @param redis redis 参数
     */
    public OrderPaidListener(NotifyService notifyService, StringRedisTemplate redis) {
        this.notifyService = notifyService;
        this.redis = redis;
    }


    /**
     * 订阅进程内事件总线。与 MQ 通道共享 SETNX 幂等键 notify:order:paid:{orderId}，
     * 双通道同时到达时仅第一个通道落库。
     * @param event 订单支付事件
     */
    @EventListener
    public void onOrderPaid(OrderPaidDTO event) {
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
            String bizId = "ORDER_PAID:" + event.getOrderId();
            notifyService.push(event.getUserId(), title, content, bizId);
            log.info("已通知用户 {} 订单 {}", event.getUserId(), event.getOrderId());
        }
        catch (Exception e) {
            // DB 落库失败时删除幂等键：否则事件重投会被 SETNX 挡掉，支付成功通知永久丢失
            redis.delete(key);
            throw e;
        }
    }
}
