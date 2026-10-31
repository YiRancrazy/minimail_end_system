package com.yirancrazy.minimall.order.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.order.service.CartCleanupService;
import com.yirancrazy.minimall.order.service.OrderOutboxPublisher;
import com.yirancrazy.minimall.order.service.OrderService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 XXL-Job 订单任务入口的锁互斥与业务委派。
 *   业务边界：使用 Mockito 模拟业务服务和 Redis，不连接 XXL-Job 调度中心。
 *   使用场景：覆盖购物车清理与发件箱转发任务。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
class OrderXxlJobHandlerTest {

  private CartCleanupService cartCleanupService;
  private OrderOutboxPublisher orderOutboxPublisher;
  private StringRedisTemplate redisTemplate;
  private OrderXxlJobHandler handler;

  /**
   * 初始化当前测试类的共享夹具。
   *
   * <p>按测试类型创建 Mockito 桩、事务模板替身或 MockMvc，并为成功路径设置宽松默认值。
   */
  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    final OrderService orderService = mock(OrderService.class);
    cartCleanupService = mock(CartCleanupService.class);
    orderOutboxPublisher = mock(OrderOutboxPublisher.class);
    redisTemplate = mock(StringRedisTemplate.class);
    ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(true);
    handler =
        new OrderXxlJobHandler(
            orderService, cartCleanupService, orderOutboxPublisher, redisTemplate, 100);
  }

  /** 验证购物车清理扫描在持锁后委派服务. */
  @Test
  void testScanCartCleanupPendingDelegatesToServiceWithLock() {
    when(cartCleanupService.scanPending()).thenReturn(2);

    handler.scanCartCleanupPending();

    verify(cartCleanupService).scanPending();
    verify(redisTemplate).delete("order:schedule:scanCartCleanupPending");
  }

  /** 验证购物车清理任务在锁被持有时跳过. */
  @Test
  void testScanCartCleanupPendingSkipsWhenLockHeld() {
    when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(false);

    handler.scanCartCleanupPending();

    verify(cartCleanupService, never()).scanPending();
  }

  /** 验证转发订单发件箱委派到发布器. */
  @Test
  void testRelayOrderOutboxDelegatesToPublisher() {
    when(orderOutboxPublisher.publishPending(100)).thenReturn(2);

    handler.relayOrderOutbox();

    verify(orderOutboxPublisher).publishPending(100);
    verify(redisTemplate).delete("order:schedule:relayOrderOutbox");
  }
}
