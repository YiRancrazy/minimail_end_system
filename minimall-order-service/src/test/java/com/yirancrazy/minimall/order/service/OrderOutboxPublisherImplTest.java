package com.yirancrazy.minimall.order.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.event.MqEventJsonCodec;
import com.yirancrazy.minimall.order.constant.OutboxSendStatusEnum;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.service.impl.OrderOutboxPublisherImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单发件箱发布器单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
class OrderOutboxPublisherImplTest {

    private OutboxManager outboxManager;
    private EventBus eventBus;
    private OrderOutboxPublisherImpl publisher;

    /**
     * 初始化测试依赖。
     */
    @BeforeEach
    void setUp() {
        outboxManager = org.mockito.Mockito.mock(OutboxManager.class);
        eventBus = org.mockito.Mockito.mock(EventBus.class);
        publisher = new OrderOutboxPublisherImpl(
            outboxManager, eventBus, 10, 30, 10, 5, 300);
    }

    /**
     * 验证发送成功后标记为已发送。
     */
    @Test
    void publishPending_marksSentWhenEventBusSucceeds() {
        OutboxPO po = pendingOrderPaid(1L, 1L, 0);
        when(outboxManager.listDispatchCandidates(any(), eq(100)))
            .thenReturn(List.of(po), List.of());
        when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);

        int sent = publisher.publishPending(100);

        assertEquals(1, sent);
        verify(eventBus).publish(any(OrderPaidDTO.class));
        verify(outboxManager).markSent(eq(po.getId()), any());
    }

    /**
     * 验证临时失败后安排重试。
     */
    @Test
    void publishPending_schedulesRetryWhenEventBusFails() {
        OutboxPO po = pendingOrderPaid(2L, 2L, 0);
        when(outboxManager.listDispatchCandidates(any(), eq(100)))
            .thenReturn(List.of(po), List.of());
        when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);
        doThrow(new IllegalStateException("broker down"))
            .when(eventBus).publish(any(OrderPaidDTO.class));

        int sent = publisher.publishPending(100);

        assertEquals(0, sent);
        verify(outboxManager).markRetry(eq(po.getId()), eq(1), any(), contains("broker down"));
        verify(outboxManager, never()).markSent(eq(po.getId()), any());
    }

    /**
     * 验证达到重试上限后进入死信。
     */
    @Test
    void publishPending_marksDeadWhenRetryLimitReached() {
        OutboxPO po = pendingOrderPaid(3L, 3L, 9);
        when(outboxManager.listDispatchCandidates(any(), eq(100)))
            .thenReturn(List.of(po), List.of());
        when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);
        doThrow(new IllegalStateException("broker down"))
            .when(eventBus).publish(any(OrderPaidDTO.class));

        publisher.publishPending(100);

        verify(outboxManager).markDead(eq(po.getId()), eq(10), contains("broker down"));
        verify(outboxManager, never()).markRetry(eq(po.getId()), eq(10), any(), any());
    }

    /**
     * 验证非法 payload 直接进入死信。
     */
    @Test
    void publishPending_marksDeadForInvalidPayload() {
        OutboxPO po = pendingOrderPaid(4L, 4L, 0);
        po.setPayload("{invalid-json");
        when(outboxManager.listDispatchCandidates(any(), eq(100)))
            .thenReturn(List.of(po), List.of());
        when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);

        publisher.publishPending(100);

        verify(outboxManager).markDead(eq(po.getId()), eq(0), any());
        verify(eventBus, never()).publish(any());
    }

    /**
     * 验证未知事件类型直接进入死信。
     */
    @Test
    void publishPending_marksDeadForUnsupportedEventType() {
        OutboxPO po = pendingOrderPaid(5L, 5L, 0);
        po.setEventType("UnsupportedEvent");
        when(outboxManager.listDispatchCandidates(any(), eq(100)))
            .thenReturn(List.of(po), List.of());
        when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);

        publisher.publishPending(100);

        verify(outboxManager).markDead(eq(po.getId()), eq(0), contains("不支持的事件类型"));
        verify(eventBus, never()).publish(any());
    }

    /**
     * 构造待发送订单支付事件。
     *
     * @param id 记录 ID
     * @param orderId 订单 ID
     * @param retryCount 重试次数
     * @return 待发送事件
     */
    private OutboxPO pendingOrderPaid(Long id, Long orderId, int retryCount) {
        OrderPaidDTO event = new OrderPaidDTO(
            orderId, 1L, new BigDecimal("10.00"), LocalDateTime.now().toString());
        OutboxPO po = new OutboxPO();
        po.setId(id);
        po.setEventId("ORDER_PAID:" + orderId);
        po.setEventType("OrderPaidDTO");
        po.setAggregateType("ORDER");
        po.setAggregateId(orderId);
        po.setTopic("minimall-events");
        po.setTag("OrderPaidDTO");
        po.setPayload(new String(MqEventJsonCodec.encode(event), StandardCharsets.UTF_8));
        po.setSendStatus(OutboxSendStatusEnum.PENDING.intCode());
        po.setRetryCount(retryCount);
        po.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
        return po;
    }
}
