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
     * 执行 onOrderPaid 方法。
     * @param event event 参数
     */
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
            notifyService.push(event.getUserId(), title, content);
            log.info("已通知用户 {} 订单 {}", event.getUserId(), event.getOrderId());
        }
        catch (Exception e) {
            // DB 落库失败时删除幂等键：否则事件重投会被 SETNX 挡掉，支付成功通知永久丢失
            redis.delete(key);
            throw e;
        }
    }
}
