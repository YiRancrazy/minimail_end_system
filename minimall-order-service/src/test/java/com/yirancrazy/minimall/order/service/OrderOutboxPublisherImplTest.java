package com.yirancrazy.minimall.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.event.MqEventJsonCodec;
import com.yirancrazy.minimall.order.constant.OutboxSendStatusEnum;
import com.yirancrazy.minimall.order.entity.OutboxPo;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.service.impl.OrderOutboxPublisherImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证发件箱发布器的抢占、路由、重试和死信推进。
 *   业务边界：使用 OutboxManager 与 EventBus 桩，固定发布器批数和重试参数。
 *   使用场景：覆盖发送成功、历史路由、路由缺失、MQ 失败、重试上限及非法事件。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
class OrderOutboxPublisherImplTest {

  private OutboxManager outboxManager;
  private EventBus eventBus;
  private OrderOutboxPublisherImpl publisher;

  /**
   * 初始化当前测试类的共享夹具。
   *
   * <p>按测试类型创建 Mockito 桩、事务模板替身或 MockMvc，并为成功路径设置宽松默认值。
   */
  @BeforeEach
  void setUp() {
    outboxManager = org.mockito.Mockito.mock(OutboxManager.class);
    eventBus = org.mockito.Mockito.mock(EventBus.class);
    publisher = new OrderOutboxPublisherImpl(outboxManager, eventBus, 10, 30, 10, 5, 300);
  }

  /** 验证事件总线成功时标记发件箱事件已发送. */
  @Test
  void testPublishPendingMarksSentWhenEventBusSucceeds() {
    OutboxPo po = pendingOrderPaid(1L, 1L, 0);
    when(outboxManager.listDispatchCandidates(any(), eq(100))).thenReturn(List.of(po), List.of());
    when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);

    int sent = publisher.publishPending(100);

    assertEquals(1, sent);
    verify(eventBus).publish(eq("minimall-events"), eq("OrderPaidDto"), any(OrderPaidDto.class));
    verify(outboxManager).markSent(eq(po.getId()), any());
  }

  /** 验证发布待处理事件时使用发件箱记录的 topic 和 tag. */
  @Test
  void testPublishPendingUsesTopicAndTagFromOutboxRecord() {
    OutboxPo po = pendingOrderPaid(6L, 6L, 0);
    po.setTopic("historical-order-events");
    po.setTag("paid-v1");
    when(outboxManager.listDispatchCandidates(any(), eq(100))).thenReturn(List.of(po), List.of());
    when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);

    publisher.publishPending(100);

    verify(eventBus).publish(eq("historical-order-events"), eq("paid-v1"), any(OrderPaidDto.class));
    verify(outboxManager).markSent(eq(po.getId()), any());
  }

  /** 验证路由缺失时将发件箱事件标记为死信. */
  @Test
  void testPublishPendingMarksDeadWhenRouteMissing() {
    OutboxPo po = pendingOrderPaid(7L, 7L, 0);
    po.setTopic(" ");
    po.setTag(null);
    when(outboxManager.listDispatchCandidates(any(), eq(100))).thenReturn(List.of(po), List.of());
    when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);

    publisher.publishPending(100);

    verify(eventBus, never()).publish(any(), any(), any());
    verify(outboxManager).markDead(eq(po.getId()), eq(0), contains("missing outbox route"));
  }

  /** 验证事件总线失败时安排发件箱重试. */
  @Test
  void testPublishPendingSchedulesRetryWhenEventBusFails() {
    OutboxPo po = pendingOrderPaid(2L, 2L, 0);
    when(outboxManager.listDispatchCandidates(any(), eq(100))).thenReturn(List.of(po), List.of());
    when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);
    doThrow(new IllegalStateException("broker down"))
        .when(eventBus)
        .publish(any(), any(), any(OrderPaidDto.class));

    int sent = publisher.publishPending(100);

    assertEquals(0, sent);
    verify(outboxManager).markRetry(eq(po.getId()), eq(1), any(), contains("broker down"));
    verify(outboxManager, never()).markSent(eq(po.getId()), any());
  }

  /** 验证达到重试上限时标记为死信. */
  @Test
  void testPublishPendingMarksDeadWhenRetryLimitReached() {
    OutboxPo po = pendingOrderPaid(3L, 3L, 9);
    when(outboxManager.listDispatchCandidates(any(), eq(100))).thenReturn(List.of(po), List.of());
    when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);
    doThrow(new IllegalStateException("broker down"))
        .when(eventBus)
        .publish(any(), any(), any(OrderPaidDto.class));

    publisher.publishPending(100);

    verify(outboxManager).markDead(eq(po.getId()), eq(10), contains("broker down"));
    verify(outboxManager, never()).markRetry(eq(po.getId()), eq(10), any(), any());
  }

  /** 验证事件载荷无效时标记为死信. */
  @Test
  void testPublishPendingMarksDeadForInvalidPayload() {
    OutboxPo po = pendingOrderPaid(4L, 4L, 0);
    po.setPayload("{invalid-json");
    when(outboxManager.listDispatchCandidates(any(), eq(100))).thenReturn(List.of(po), List.of());
    when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);

    publisher.publishPending(100);

    verify(outboxManager).markDead(eq(po.getId()), eq(0), any());
    verify(eventBus, never()).publish(any(), any(), any());
  }

  /** 验证事件类型不支持时标记为死信. */
  @Test
  void testPublishPendingMarksDeadForUnsupportedEventType() {
    OutboxPo po = pendingOrderPaid(5L, 5L, 0);
    po.setEventType("UnsupportedEvent");
    when(outboxManager.listDispatchCandidates(any(), eq(100))).thenReturn(List.of(po), List.of());
    when(outboxManager.claimForDispatch(eq(po.getId()), any(), any())).thenReturn(true);

    publisher.publishPending(100);

    verify(outboxManager).markDead(eq(po.getId()), eq(0), contains("不支持的事件类型"));
    verify(eventBus, never()).publish(any(), any(), any());
  }

  /**
   * 构造待发送订单支付事件.
   *
   * @param id 记录 ID
   * @param orderId 订单 ID
   * @param retryCount 重试次数
   * @return 待发送事件
   */
  private OutboxPo pendingOrderPaid(Long id, Long orderId, int retryCount) {
    OrderPaidDto event =
        new OrderPaidDto(orderId, 1L, new BigDecimal("10.00"), LocalDateTime.now().toString());
    OutboxPo po = new OutboxPo();
    po.setId(id);
    po.setEventId("ORDER_PAID:" + orderId);
    po.setEventType("OrderPaidDto");
    po.setAggregateType("ORDER");
    po.setAggregateId(orderId);
    po.setTopic("minimall-events");
    po.setTag("OrderPaidDto");
    po.setPayload(new String(MqEventJsonCodec.encode(event), StandardCharsets.UTF_8));
    po.setSendStatus(OutboxSendStatusEnum.PENDING.intCode());
    po.setRetryCount(retryCount);
    po.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
    return po;
  }
}
