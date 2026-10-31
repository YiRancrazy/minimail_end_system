package com.yirancrazy.minimall.notify.listener;

import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.service.NotifyService;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaidMqConsumerTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
        lenient().when(valueOps.get(anyString())).thenReturn(null);
        lenient().when(notifyService.push(any(Long.class), anyString(), anyString(), anyString()))
            .thenReturn(true);
        consumer = new OrderPaidMqConsumer(notifyService, redis,
            "localhost:9876", "events", "notify-test");
    }

    @Test
    void onPaid_givenNormalEvent_thenPushesNotification() {
        consumer.onPaid(paidEvent());

        String content = "订单 99 已支付，金额 100.00";
        verify(notifyService).push(7L, "订单支付成功", content, "ORDER_PAID:99");
        verify(valueOps).setIfAbsent(
            "notify:order:paid:99", "1", Duration.ofDays(7));
    }

    @Test
    void onPaid_givenNullEvent_thenDoesNothing() {
        consumer.onPaid(null);

        verifyNoInteractions(notifyService);
    }

    @Test
    void onPaid_givenNullOrderId_thenDoesNothing() {
        consumer.onPaid(new OrderPaidDTO(
            null, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00"));

        verifyNoInteractions(notifyService);
    }


    @Test
    void onPaid_givenDuplicateEvent_thenSkipsNotification() {
        when(valueOps.get("notify:order:paid:99")).thenReturn("1");

        consumer.onPaid(paidEvent());

        verifyNoInteractions(notifyService);
        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }


    @Test
    void onPaid_givenPushFailure_thenRethrowsWithoutCaching() {
        when(notifyService.push(any(Long.class), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("db unavailable"));

        assertThrows(RuntimeException.class, () -> consumer.onPaid(paidEvent()));

        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    /**
     * 验证数据库保存失败时抛出业务异常并触发重投。
     */
    @Test
    void onPaid_givenPushReturnsFalse_thenThrowsBizException() {
        when(notifyService.push(any(Long.class), anyString(), anyString(), anyString()))
            .thenReturn(false);

        BizException exception = assertThrows(BizException.class,
            () -> consumer.onPaid(paidEvent()));

        assertEquals(NotifyCodeEnum.NOTIFY_SAVE_FAILED.getCode(), exception.getCode());
    }

    /**
     * 验证 Redis 读取失败时降级到数据库处理。
     */
    @Test
    void onPaid_givenRedisReadFailure_thenContinuesWithDatabase() {
        when(valueOps.get(anyString())).thenThrow(new IllegalStateException("redis down"));

        assertDoesNotThrow(() -> consumer.onPaid(paidEvent()));

        verify(notifyService).push(7L, "订单支付成功",
            "订单 99 已支付，金额 100.00", "ORDER_PAID:99");
    }

    /**
     * 验证 Redis 写入失败不影响消息消费。
     */
    @Test
    void onPaid_givenRedisWriteFailure_thenCompletesSuccessfully() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenThrow(new IllegalStateException("redis down"));

        assertDoesNotThrow(() -> consumer.onPaid(paidEvent()));

        verify(notifyService).push(7L, "订单支付成功",
            "订单 99 已支付，金额 100.00", "ORDER_PAID:99");
    }

    /**
     * 构造订单支付事件。
     *
     * @return 订单支付事件
     */
    private OrderPaidDTO paidEvent() {
        return new OrderPaidDTO(
            99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");
    }
}
