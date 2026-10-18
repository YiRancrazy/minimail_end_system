package com.yirancrazy.minimall.notify.listener;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        lenient().when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Boolean.TRUE);
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
    public void on_order_paid_saves_message_and_pushes_sse() {
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");
        listener.onOrderPaid(ev);

        ArgumentCaptor<NotifyMessagePO> cap = ArgumentCaptor.forClass(NotifyMessagePO.class);
        verify(manager, times(1)).save(cap.capture());
        assertEquals(7L, cap.getValue().getUserId());
        assertNotNull(cap.getValue().getTitle());
        assertNotNull(cap.getValue().getContent());

        verify(notifyPushGateway, times(1)).send(eq(7L), anyString());
    }


    /**
     * 执行 on_order_paid_duplicate_skips_notification 方法。
     */
    public void on_order_paid_duplicate_skips_notification() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Boolean.FALSE);
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        listener.onOrderPaid(ev);

        verify(manager, never()).save(any(NotifyMessagePO.class));
        verify(notifyPushGateway, never()).send(any(), any());
    }


    /**
     * 执行 on_order_paid_push_failure_removes_key_and_rethrows 方法。
     */
    public void on_order_paid_push_failure_removes_key_and_rethrows() {
        doAnswer(inv -> {
            throw new RuntimeException("db unavailable");
        }).when(manager).save(any(NotifyMessagePO.class));
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
            () -> listener.onOrderPaid(ev));

        verify(redis).delete("notify:order:paid:99");
    }
}
