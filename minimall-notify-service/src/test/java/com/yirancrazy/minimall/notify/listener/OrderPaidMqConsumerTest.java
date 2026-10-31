package com.yirancrazy.minimall.notify.listener;

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

import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import com.yirancrazy.minimall.notify.service.NotifyService;
import java.math.BigDecimal;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证订单支付消息消费者的正常推送、空事件跳过、Redis/数据库双重防重以及 Redis 故障降级行为.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class OrderPaidMqConsumerTest {

  private NotifyService notifyService;

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);

  private StringRedisTemplate redis;
  private OrderPaidMqConsumer consumer;

  /** 创建通知服务和 Redis Mock，配置默认成功推送，并构造测试消费组实例。 */
  @BeforeEach
  void setUp() {
    notifyService = mock(NotifyService.class);
    redis = mock(StringRedisTemplate.class);
    lenient().when(redis.opsForValue()).thenReturn(valueOps);
    lenient().when(valueOps.get(anyString())).thenReturn(null);
    lenient()
        .when(notifyService.push(any(Long.class), anyString(), anyString(), anyString()))
        .thenReturn(true);
    consumer =
        new OrderPaidMqConsumer(notifyService, redis, "localhost:9876", "events", "notify-test");
  }

  /** 验证Redis无键且通知保存成功时，正常支付事件写入通知并设置7天去重键. */
  @Test
  void testOnPaidGivenNormalEventThenPushesNotification() {
    consumer.onPaid(paidEvent());

    String content = "订单 99 已支付，金额 100.00";
    verify(notifyService).push(7L, "订单支付成功", content, "ORDER_PAID:99");
    verify(valueOps).setIfAbsent("notify:order:paid:99", "1", Duration.ofDays(7));
  }

  /** 验证收到空支付事件时不调用通知服务且不产生副作用. */
  @Test
  void testOnPaidGivenNullEventThenDoesNothing() {
    consumer.onPaid(null);

    verifyNoInteractions(notifyService);
  }

  /** 验证支付事件订单编号为空时不调用通知服务. */
  @Test
  void testOnPaidGivenNullOrderIdThenDoesNothing() {
    consumer.onPaid(new OrderPaidDto(null, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00"));

    verifyNoInteractions(notifyService);
  }

  /** 验证Redis已存在订单去重键时不写通知且不重复设置键. */
  @Test
  void testOnPaidGivenDuplicateEventThenSkipsNotification() {
    when(valueOps.get("notify:order:paid:99")).thenReturn("1");

    consumer.onPaid(paidEvent());

    verifyNoInteractions(notifyService);
    verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
  }

  /** 验证通知写入抛运行时异常时异常上抛且不缓存去重键. */
  @Test
  void testOnPaidGivenPushFailureThenRethrowsWithoutCaching() {
    when(notifyService.push(any(Long.class), anyString(), anyString(), anyString()))
        .thenThrow(new RuntimeException("db unavailable"));

    assertThrows(RuntimeException.class, () -> consumer.onPaid(paidEvent()));

    verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
  }

  /** 验证通知保存返回false时抛出通知保存失败业务异常. */
  @Test
  void testOnPaidGivenPushReturnsFalseThenThrowsBizException() {
    when(notifyService.push(any(Long.class), anyString(), anyString(), anyString()))
        .thenReturn(false);

    BizException exception = assertThrows(BizException.class, () -> consumer.onPaid(paidEvent()));

    assertEquals(NotifyCodeEnum.NOTIFY_SAVE_FAILED.getCode(), exception.getCode());
  }

  /** 验证Redis读取抛出异常时降级继续写通知并完成支付通知. */
  @Test
  void testOnPaidGivenRedisReadFailureThenContinuesWithDatabase() {
    when(valueOps.get(anyString())).thenThrow(new IllegalStateException("redis down"));

    assertDoesNotThrow(() -> consumer.onPaid(paidEvent()));

    verify(notifyService).push(7L, "订单支付成功", "订单 99 已支付，金额 100.00", "ORDER_PAID:99");
  }

  /** 验证Redis去重键写入失败时忽略异常并保持通知处理成功. */
  @Test
  void testOnPaidGivenRedisWriteFailureThenCompletesSuccessfully() {
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenThrow(new IllegalStateException("redis down"));

    assertDoesNotThrow(() -> consumer.onPaid(paidEvent()));

    verify(notifyService).push(7L, "订单支付成功", "订单 99 已支付，金额 100.00", "ORDER_PAID:99");
  }

  /**
   * 构造订单支付事件.
   *
   * @return 订单支付事件
   */
  private OrderPaidDto paidEvent() {
    return new OrderPaidDto(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");
  }
}
