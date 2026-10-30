package com.yirancrazy.minimall.order.schedule;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.order.service.CartCleanupService;
import com.yirancrazy.minimall.order.service.OrderOutboxPublisher;
import com.yirancrazy.minimall.order.service.OrderService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderScheduleHandlerTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class OrderScheduleHandlerTest {

    private OrderScheduleHandler handler;
    private OrderService orderService;
    private CartCleanupService cartCleanupService;
    private OrderOutboxPublisher orderOutboxPublisher;
    private StringRedisTemplate redisTemplate;

    @SuppressWarnings("unchecked")
    private void setUpHandler() {
        orderService = mock(OrderService.class);
        cartCleanupService = mock(CartCleanupService.class);
        orderOutboxPublisher = mock(OrderOutboxPublisher.class);
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        handler = new OrderScheduleHandler(
            orderService, cartCleanupService, orderOutboxPublisher, redisTemplate, 100);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanExpiredOrders_delegates_to_service() {
        setUpHandler();
        when(orderService.scanExpiredOrders()).thenReturn(3);

        handler.scanExpiredOrders();

        verify(orderService, times(1)).scanExpiredOrders();
        assertEquals(3, orderService.scanExpiredOrders());
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanAutoConfirm_delegates_to_service() {
        setUpHandler();
        when(orderService.scanAutoConfirm()).thenReturn(5);

        handler.scanAutoConfirm();

        verify(orderService, times(1)).scanAutoConfirm();
        assertEquals(5, orderService.scanAutoConfirm());
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanStockReleasePending_delegates_to_service() {
        setUpHandler();
        when(orderService.scanStockReleasePending()).thenReturn(2);

        handler.scanStockReleasePending();

        verify(orderService, times(1)).scanStockReleasePending();
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanExpiredOrders_skips_when_lock_held() {
        setUpHandler();
        when(orderService.scanExpiredOrders()).thenReturn(3);
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false);

        handler.scanExpiredOrders();

        verify(orderService, times(0)).scanExpiredOrders();
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanExpiredOrders_releases_lock_with_holder_token() {
        setUpHandler();
        when(orderService.scanExpiredOrders()).thenReturn(1);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), tokenCaptor.capture(), ttlCaptor.capture()))
            .thenReturn(true);

        handler.scanExpiredOrders();

        verify(redisTemplate).execute(any(RedisScript.class), anyList(), eq(tokenCaptor.getValue()));
        assertTrue(ttlCaptor.getValue().compareTo(Duration.ofMinutes(1)) > 0,
            "lock ttl must cover a batched scan");
    }


    /**
     * 购物车清理扫描应委派给服务。
     */
    @Test
    void scanCartCleanupPending_delegatesToServiceWithLock() {
        setUpHandler();
        when(cartCleanupService.scanPending()).thenReturn(2);

        handler.scanCartCleanupPending();

        verify(cartCleanupService).scanPending();
    }


    /**
     * 分布式锁被占用时跳过购物车清理扫描。
     */
    @Test
    void scanCartCleanupPending_skipsWhenLockHeld() {
        setUpHandler();
        when(redisTemplate.opsForValue().setIfAbsent(
            anyString(), anyString(), any(Duration.class))).thenReturn(false);

        handler.scanCartCleanupPending();

        verify(cartCleanupService, times(0)).scanPending();
    }


    /**
     * 发件箱扫描应委派给发布器。
     */
    @Test
    void relayOrderOutbox_delegatesToPublisher() {
        setUpHandler();
        when(orderOutboxPublisher.publishPending(100)).thenReturn(3);

        handler.relayOrderOutbox();

        verify(orderOutboxPublisher).publishPending(100);
    }
}
