package com.yirancrazy.minimall.order.schedule;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.order.service.CartCleanupService;
import com.yirancrazy.minimall.order.service.OrderOutboxPublisher;
import com.yirancrazy.minimall.order.service.OrderService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: XXL-Job 购物车清理扫描测试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
class OrderXxlJobHandlerTest {

    private CartCleanupService cartCleanupService;
    private OrderOutboxPublisher orderOutboxPublisher;
    private StringRedisTemplate redisTemplate;
    private OrderXxlJobHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        OrderService orderService = mock(OrderService.class);
        cartCleanupService = mock(CartCleanupService.class);
        orderOutboxPublisher = mock(OrderOutboxPublisher.class);
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);
        handler = new OrderXxlJobHandler(
            orderService, cartCleanupService, orderOutboxPublisher, redisTemplate, 100);
    }

    @Test
    void scanCartCleanupPending_delegatesToServiceWithLock() {
        when(cartCleanupService.scanPending()).thenReturn(2);

        handler.scanCartCleanupPending();

        verify(cartCleanupService).scanPending();
        verify(redisTemplate).delete("order:schedule:scanCartCleanupPending");
    }

    @Test
    void scanCartCleanupPending_skipsWhenLockHeld() {
        when(redisTemplate.opsForValue().setIfAbsent(
            anyString(), anyString(), any(Duration.class))).thenReturn(false);

        handler.scanCartCleanupPending();

        verify(cartCleanupService, never()).scanPending();
    }

    @Test
    void relayOrderOutbox_delegatesToPublisher() {
        when(orderOutboxPublisher.publishPending(100)).thenReturn(2);

        handler.relayOrderOutbox();

        verify(orderOutboxPublisher).publishPending(100);
        verify(redisTemplate).delete("order:schedule:relayOrderOutbox");
    }
}
