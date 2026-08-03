package com.yirancrazy.minimall.notify.listener;

import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaidMqConsumer 的单元测试类。
 * @Version: 1.2
 * @DateTime: 2026/08/03
 **/
class OrderPaidMqConsumerTest {

    private NotifyService notifyService;
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private StringRedisTemplate redis;
    private OrderPaidMqConsumer consumer;

    @BeforeEach
    void setUp() {
        notifyService = mock(NotifyService.class);
        redis = mock(StringRedisTemplate.class);
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Boolean.TRUE);
        consumer = new OrderPaidMqConsumer(notifyService, redis,
            "localhost:9876", "events", "notify-test");
    }

    @Test
    void onPaid_givenNormalEvent_thenPushesNotification() {
        OrderPaidDTO event = new OrderPaidDTO(
            99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        consumer.onPaid(event);

        String content = "订单 99 已支付，金额 100.00";
        verify(notifyService).push(7L, "订单支付成功", content);
    }

    @Test
    void onPaid_givenNullEvent_thenDoesNothing() {
        consumer.onPaid(null);

        verifyNoInteractions(notifyService);
    }

    @Test
    void onPaid_givenNullOrderId_thenDoesNothing() {
        OrderPaidDTO event = new OrderPaidDTO(
            null, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        consumer.onPaid(event);

        verifyNoInteractions(notifyService);
    }

    /**
     * 验证 SETNX 返回 false（重复事件）时跳过通知。
     */
    @Test
    void onPaid_givenDuplicateEvent_thenSkipsNotification() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Boolean.FALSE);
        OrderPaidDTO event = new OrderPaidDTO(
            99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        consumer.onPaid(event);

        verifyNoInteractions(notifyService);
    }
}
