package com.yirancrazy.minimall.notify.listener;

import java.time.Duration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
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
     * 订阅进程内事件总线。与 MQ 通道共享处理完成标记 notify:order:paid:{orderId}，
     * 数据库唯一键负责最终幂等。
     * @param event 订单支付事件
     */
    @EventListener
    public void onOrderPaid(OrderPaidDTO event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        String key = "notify:order:paid:" + event.getOrderId();
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

        if (pushResult) {
            try {
                redis.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
            }
            catch (Exception e) {
                log.warn("Redis 写入失败, 降级到数据库唯一键兜底, key={}", key, e);
            }
        }
        else {
            throw new BizException(NotifyCodeEnum.NOTIFY_SAVE_FAILED);
        }

        log.info("已通知用户 {} 订单 {}", event.getUserId(), event.getOrderId());
    }
}
