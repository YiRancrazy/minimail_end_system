package com.yirancrazy.minimall.notify.listener;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.gateway.NotifyPushGateway;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.service.impl.NotifyServiceImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaidListenerTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class OrderPaidListenerTest {

    private NotifyManager manager;
    private NotifyPushGateway notifyPushGateway;
    private StringRedisTemplate redis;
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private NotifyService notifyService;
    private OrderPaidListener listener;

    @BeforeEach
    void setUp() {
        manager = mock(NotifyManager.class);
        notifyPushGateway = mock(NotifyPushGateway.class);
        redis = mock(StringRedisTemplate.class);
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenReturn(null);
        lenient().when(manager.list((Wrapper<NotifyMessagePO>) any())).thenReturn(Collections.emptyList());
        doAnswer(inv -> {
            NotifyMessagePO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(NotifyMessagePO.class));
        notifyService = new NotifyServiceImpl(manager, notifyPushGateway);
        listener = new OrderPaidListener(notifyService, redis);
    }


    /**
     * 执行 on_order_paid_saves_message_and_pushes_sse 方法。
     */
    @Test
    public void on_order_paid_saves_message_and_pushes_sse() {
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");
        listener.onOrderPaid(ev);

        ArgumentCaptor<NotifyMessagePO> cap = ArgumentCaptor.forClass(NotifyMessagePO.class);
        verify(manager, times(1)).save(cap.capture());
        assertEquals(7L, cap.getValue().getUserId());
        assertNotNull(cap.getValue().getTitle());
        assertNotNull(cap.getValue().getContent());

        verify(notifyPushGateway, times(1)).send(eq(7L), anyString());
        verify(valueOps).setIfAbsent(
            "notify:order:paid:99", "1", Duration.ofDays(7));
    }


    /**
     * 执行 on_order_paid_duplicate_skips_notification 方法。
     */
    @Test
    public void on_order_paid_duplicate_skips_notification() {
        when(valueOps.get("notify:order:paid:99")).thenReturn("1");
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        listener.onOrderPaid(ev);

        verify(manager, never()).save(any(NotifyMessagePO.class));
        verify(notifyPushGateway, never()).send(any(), any());
    }


    /**
     * 验证数据库保存失败时直接向外抛出异常。
     */
    @Test
    public void on_order_paid_push_failure_rethrows_without_deleting_key() {
        doAnswer(inv -> {
            throw new RuntimeException("db unavailable");
        }).when(manager).save(any(NotifyMessagePO.class));
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        assertThrows(RuntimeException.class, () -> listener.onOrderPaid(ev));

        verify(redis, never()).delete(anyString());
    }

    /**
     * 验证数据库保存返回 false 时抛出业务异常。
     */
    @Test
    public void on_order_paid_save_returns_false_then_throws_biz_exception() {
        when(manager.save(any(NotifyMessagePO.class))).thenReturn(false);
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        BizException exception = assertThrows(BizException.class,
            () -> listener.onOrderPaid(ev));

        assertEquals(NotifyCodeEnum.NOTIFY_SAVE_FAILED.getCode(), exception.getCode());
        verify(notifyPushGateway, never()).send(any(), any());
    }

    /**
     * 验证 Redis 读取失败时继续数据库处理。
     */
    @Test
    public void on_order_paid_redis_read_failure_then_continues_database() {
        when(valueOps.get(anyString())).thenThrow(new IllegalStateException("redis down"));
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        assertDoesNotThrow(() -> listener.onOrderPaid(ev));

        verify(manager).save(any(NotifyMessagePO.class));
        verify(notifyPushGateway).send(eq(7L), anyString());
    }

    /**
     * 验证 Redis 写入失败时不影响通知处理结果。
     */
    @Test
    public void on_order_paid_redis_write_failure_then_completes_successfully() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenThrow(new IllegalStateException("redis down"));
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        assertDoesNotThrow(() -> listener.onOrderPaid(ev));

        verify(manager).save(any(NotifyMessagePO.class));
        verify(notifyPushGateway).send(eq(7L), anyString());
    }
}
