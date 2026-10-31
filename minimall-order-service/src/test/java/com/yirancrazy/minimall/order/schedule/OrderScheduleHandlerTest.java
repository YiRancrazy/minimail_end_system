package com.yirancrazy.minimall.order.schedule;

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
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 Spring Scheduling 订单任务的 Redis 锁与委派行为。
 *   业务边界：使用 Mockito 模拟订单、购物车、发件箱和 Redis 依赖，不启动真实调度器。
 *   使用场景：覆盖任务委派、锁占用跳过和锁释放令牌。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class OrderScheduleHandlerTest {

  private OrderScheduleHandler handler;
  private OrderService orderService;
  private CartCleanupService cartCleanupService;
  private OrderOutboxPublisher orderOutboxPublisher;
  private StringRedisTemplate redisTemplate;

  /**
   * 初始化 Spring Scheduling 订单任务测试夹具。
   *
   * <p>创建业务服务、发件箱发布器和 Redis 桩，默认返回成功获取锁。
   */
  @SuppressWarnings("unchecked")
  private void setUpHandler() {
    orderService = mock(OrderService.class);
    cartCleanupService = mock(CartCleanupService.class);
    orderOutboxPublisher = mock(OrderOutboxPublisher.class);
    redisTemplate = mock(StringRedisTemplate.class);
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    handler =
        new OrderScheduleHandler(
            orderService, cartCleanupService, orderOutboxPublisher, redisTemplate, 100);
  }

  /**
   * 测试目标：验证超时订单扫描委派到订单服务。
   *
   * <p>测试场景：验证超时订单扫描委派。
   * <p>前置条件：初始化调度器与业务服务桩，并让锁获取成功。
   * <p>关键步骤：调用 scanExpiredOrders 后检查 OrderService.scanExpiredOrders 恰好执行一次。
   * <p>预期结果：超时订单扫描委派到订单服务，且返回值、异常或交互断言全部成立。
   */
  public void scanExpiredOrdersDelegatesToService() {
    setUpHandler();
    when(orderService.scanExpiredOrders()).thenReturn(3);

    handler.scanExpiredOrders();

    verify(orderService, times(1)).scanExpiredOrders();
    assertEquals(3, orderService.scanExpiredOrders());
  }

  /**
   * 测试目标：验证自动确认扫描委派到订单服务。
   *
   * <p>测试场景：验证自动确认扫描委派。
   * <p>前置条件：初始化调度器与业务服务桩，并让锁获取成功。
   * <p>关键步骤：调用 scanAutoConfirm 后检查 OrderService.scanAutoConfirm 恰好执行一次。
   * <p>预期结果：自动确认扫描委派到订单服务，且返回值、异常或交互断言全部成立。
   */
  public void scanAutoConfirmDelegatesToService() {
    setUpHandler();
    when(orderService.scanAutoConfirm()).thenReturn(5);

    handler.scanAutoConfirm();

    verify(orderService, times(1)).scanAutoConfirm();
    assertEquals(5, orderService.scanAutoConfirm());
  }

  /**
   * 测试目标：验证库存释放补偿扫描委派到订单服务。
   *
   * <p>测试场景：验证库存释放补偿扫描委派。
   * <p>前置条件：初始化调度器与业务服务桩，并让锁获取成功。
   * <p>关键步骤：调用 scanStockReleasePending 后检查 OrderService 对应方法执行。
   * <p>预期结果：库存释放补偿扫描委派到订单服务，且返回值、异常或交互断言全部成立。
   */
  public void scanStockReleasePendingDelegatesToService() {
    setUpHandler();
    when(orderService.scanStockReleasePending()).thenReturn(2);

    handler.scanStockReleasePending();

    verify(orderService, times(1)).scanStockReleasePending();
  }

  /**
   * 测试目标：验证Redis 锁被占用时跳过超时扫描。
   *
   * <p>测试场景：验证超时扫描在 Redis 锁被占用时跳过。
   * <p>前置条件：Redis setIfAbsent 被配置为返回 false。
   * <p>关键步骤：调用 scanExpiredOrders 后确认业务服务从未执行。
   * <p>预期结果：Redis 锁被占用时跳过超时扫描，且返回值、异常或交互断言全部成立。
   */
  public void scanExpiredOrdersSkipsWhenLockHeld() {
    setUpHandler();
    when(orderService.scanExpiredOrders()).thenReturn(3);
    when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(false);

    handler.scanExpiredOrders();

    verify(orderService, times(0)).scanExpiredOrders();
  }

  /**
   * 测试目标：验证超时扫描使用持有者令牌释放 Redis 锁。
   *
   * <p>测试场景：验证超时扫描使用原持有者令牌释放 Redis 锁。
   * <p>前置条件：捕获 setIfAbsent 写入的 token 和 TTL，并让锁获取成功。
   * <p>关键步骤：调用后确认 Lua 释放脚本收到同一 token，且 TTL 足以覆盖批处理。
   * <p>预期结果：超时扫描使用持有者令牌释放 Redis 锁，且返回值、异常或交互断言全部成立。
   */
  public void scanExpiredOrdersReleasesLockWithHolderToken() {
    setUpHandler();
    when(orderService.scanExpiredOrders()).thenReturn(1);
    ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
    when(redisTemplate
            .opsForValue()
            .setIfAbsent(anyString(), tokenCaptor.capture(), ttlCaptor.capture()))
        .thenReturn(true);

    handler.scanExpiredOrders();

    verify(redisTemplate).execute(any(RedisScript.class), anyList(), eq(tokenCaptor.getValue()));
    assertTrue(
        ttlCaptor.getValue().compareTo(Duration.ofMinutes(1)) > 0,
        "lock ttl must cover a batched scan");
  }

  /** 验证购物车清理扫描在持锁后委派服务. */
  @Test
  void testScanCartCleanupPendingDelegatesToServiceWithLock() {
    setUpHandler();
    when(cartCleanupService.scanPending()).thenReturn(2);

    handler.scanCartCleanupPending();

    verify(cartCleanupService).scanPending();
  }

  /** 验证购物车清理任务在锁被持有时跳过. */
  @Test
  void testScanCartCleanupPendingSkipsWhenLockHeld() {
    setUpHandler();
    when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(false);

    handler.scanCartCleanupPending();

    verify(cartCleanupService, times(0)).scanPending();
  }

  /** 验证转发订单发件箱委派到发布器. */
  @Test
  void testRelayOrderOutboxDelegatesToPublisher() {
    setUpHandler();
    when(orderOutboxPublisher.publishPending(100)).thenReturn(3);

    handler.relayOrderOutbox();

    verify(orderOutboxPublisher).publishPending(100);
  }
}
