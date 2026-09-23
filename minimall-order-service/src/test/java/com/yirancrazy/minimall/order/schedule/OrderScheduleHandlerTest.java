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
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * OrderScheduleHandler 单元测试，验证定时任务加锁后委托调用 OrderService 扫描方法。
 */
public class OrderScheduleHandlerTest {

    private OrderScheduleHandler handler;
    private OrderService orderService;
    private StringRedisTemplate redisTemplate;

    @SuppressWarnings("unchecked")
    private void setUpHandler() {
        orderService = mock(OrderService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        handler = new OrderScheduleHandler(orderService, redisTemplate);
    }

    /**
     * 验证 scanExpiredOrders 获取锁成功后委托 OrderService.scanExpiredOrders 并处理返回值。
     */
    @Test
    public void scanExpiredOrders_delegates_to_service() {
        setUpHandler();
        when(orderService.scanExpiredOrders()).thenReturn(3);

        handler.scanExpiredOrders();

        verify(orderService, times(1)).scanExpiredOrders();
        assertEquals(3, orderService.scanExpiredOrders());
    }

    /**
     * 验证 scanAutoConfirm 获取锁成功后委托 OrderService.scanAutoConfirm。
     */
    @Test
    public void scanAutoConfirm_delegates_to_service() {
        setUpHandler();
        when(orderService.scanAutoConfirm()).thenReturn(5);

        handler.scanAutoConfirm();

        verify(orderService, times(1)).scanAutoConfirm();
        assertEquals(5, orderService.scanAutoConfirm());
    }

    /**
     * 验证库存释放补偿扫描获取锁成功后委托 OrderService.scanStockReleasePending。
     */
    @Test
    public void scanStockReleasePending_delegates_to_service() {
        setUpHandler();
        when(orderService.scanStockReleasePending()).thenReturn(2);

        handler.scanStockReleasePending();

        verify(orderService, times(1)).scanStockReleasePending();
    }

    /**
     * 验证加锁失败（其他实例持有锁）时跳过扫描，不触发 service 调用。
     */
    @Test
    public void scanExpiredOrders_skips_when_lock_held() {
        setUpHandler();
        when(orderService.scanExpiredOrders()).thenReturn(3);
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false);

        handler.scanExpiredOrders();

        verify(orderService, times(0)).scanExpiredOrders();
    }

    /**
     * 验证释放锁使用本实例持有者标识做原子比较删除，且锁 TTL 已覆盖分批扫描耗时。
     */
    @Test
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
}
