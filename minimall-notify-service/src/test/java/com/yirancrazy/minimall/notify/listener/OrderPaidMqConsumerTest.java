package com.yirancrazy.minimall.notify.listener;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.sse.SseHub;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaidMqConsumer 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
class OrderPaidMqConsumerTest {

    private NotifyService notifyService;
    private SseHub sseHub;
    private OrderPaidMqConsumer consumer;

    @BeforeEach
    void setUp() {
        notifyService = mock(NotifyService.class);
        sseHub = mock(SseHub.class);
        consumer = new OrderPaidMqConsumer(notifyService, sseHub, "localhost:9876", "events", "notify-test");
    }

    @Test
    void onPaid_givenNormalEvent_thenPushesNotificationAndSse() {
        OrderPaidDTO event = new OrderPaidDTO(
            99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        consumer.onPaid(event);

        String content = "订单 99 已支付，金额 100.00";
        verify(notifyService).push(7L, "订单支付成功", content);
        verify(sseHub).send(7L, "订单支付成功: " + content);
    }

    @Test
    void onPaid_givenNullEvent_thenDoesNothing() {
        consumer.onPaid(null);

        verifyNoInteractions(notifyService, sseHub);
    }

    @Test
    void onPaid_givenNullOrderId_thenDoesNothing() {
        OrderPaidDTO event = new OrderPaidDTO(
            null, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        consumer.onPaid(event);

        verifyNoInteractions(notifyService, sseHub);
    }
}