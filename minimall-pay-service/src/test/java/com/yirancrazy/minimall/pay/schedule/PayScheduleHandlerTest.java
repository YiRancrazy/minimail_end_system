package com.yirancrazy.minimall.pay.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.pay.service.PayService;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 验证支付补偿任务与 Redis 分布式锁的协作行为。
 * 使用最小 Spring 上下文、Mock PayService 和 Redis Mock，不连接数据库或真实 Redis。
 * @Version: 1.0
 * @DateTime: 2026/10/21 20:04
 */
@SpringBootTest(
    classes = PayScheduleHandlerTest.MinimalContext.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      // 开启 PayScheduleHandler Bean。
      "minimall.schedule.enabled=true",
      // 测试仅手工调用方法，不开启调度基础设施。
      "minimall.xxl-job.enabled=false"
    })
@SuppressWarnings("unchecked")
public class PayScheduleHandlerTest {

  private static final String PAID_KEY = "pay:schedule:scanPaidButOrderPending";
  private static final String UNNOTIFIED_KEY = "pay:schedule:scanUnnotifiedRefunds";
  private static final String PENDING_KEY = "pay:schedule:scanPendingRefunds";

  // 扫描业务依赖，使用 Mock 隔离真实支付业务。
  @MockBean private PayService payService;

  // Redis 客户端使用 Mock，不连接真实 Redis。
  @MockBean private StringRedisTemplate redisTemplate;

  // 模拟 StringRedisTemplate.opsForValue()。
  @MockBean private ValueOperations<String, String> valueOperations;

  // 被测对象必须是真实 Bean，不能使用 @MockBean。
  @Autowired private PayScheduleHandler handler;

  /**
   * 初始化 Redis 客户端 Mock 的默认加锁成功行为。
   * 每个场景执行前，`opsForValue()` 返回同一值操作替身，且 `setIfAbsent` 默认返回 true。
   */
  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(true);
  }

  /**
   * @Author: YiRanCrazy@gmail.com
   * @Description: 仅为定时任务测试加载 `PayScheduleHandler`。
   * 限制 Spring 扫描范围，并显式关闭 XXL-Job 调度基础设施以隔离被测逻辑。
   * @Version: 1.0
   * @DateTime: 2026/10/21 20:04
   */
  @Configuration(proxyBeanMethods = false)
  @Import(PayScheduleHandler.class)
  static class MinimalContext {}

  /** 验证补偿调度：三类锁均获取成功，预期三类扫描执行并释放锁. */
  @Test
  void testExecuteAllScansWhenLocksAcquired() {
    invokeAll();

    verify(payService).scanPaidButOrderPending();
    verify(payService).scanUnnotifiedRefunds();
    verify(payService).scanPendingRefunds();
    verify(redisTemplate, times(3)).execute(any(RedisScript.class), anyList(), anyString());
  }

  /** 验证补偿调度：三类锁均被占用，预期跳过扫描且不执行解锁. */
  @Test
  void testSkipAllScansWhenLocksHeld() {
    when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
        .thenReturn(false);

    invokeAll();

    verifyNoInteractions(payService);
    verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), anyString());
  }

  /** 验证补偿调度：执行三类任务，预期加锁键集合恰含三个独立键. */
  @Test
  void testUseDistinctLockKeys() {
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    when(valueOperations.setIfAbsent(keyCaptor.capture(), anyString(), any(Duration.class)))
        .thenReturn(true);

    invokeAll();

    assertEquals(
        Set.of(PAID_KEY, UNNOTIFIED_KEY, PENDING_KEY), new HashSet<>(keyCaptor.getAllValues()));
  }

  /** 验证补偿调度：待退款任务加锁解锁，预期租约十分钟且令牌一致. */
  @Test
  void testReleaseWithSameTokenAndExpectedTtl() {
    ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
    when(valueOperations.setIfAbsent(
            eq(PENDING_KEY), tokenCaptor.capture(), eq(Duration.ofMinutes(10))))
        .thenReturn(true);

    handler.scanPendingRefunds();

    verify(redisTemplate)
        .execute(any(RedisScript.class), eq(List.of(PENDING_KEY)), eq(tokenCaptor.getValue()));
  }

  /** 验证补偿调度：扫描抛异常，预期异常外传且仍执行一次解锁. */
  @Test
  void testReleaseWhenScanThrows() {
    when(payService.scanPendingRefunds()).thenThrow(new IllegalStateException("scan failed"));

    assertThrows(IllegalStateException.class, handler::scanPendingRefunds);

    verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(PENDING_KEY)), anyString());
  }

  /**
   * 手工调用三个补偿扫描方法，供锁行为测试共享执行步骤。
   * 调用顺序为已支付未推进、未通知退款、待退款，不启动真实调度线程。
   */
  private void invokeAll() {
    handler.scanPaidButOrderPending();
    handler.scanUnnotifiedRefunds();
    handler.scanPendingRefunds();
  }
}
