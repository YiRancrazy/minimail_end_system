package com.yirancrazy.minimall.common.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 MdcTaskDecorator 在工作线程中的上下文复制、空上下文清理和原上下文恢复。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class MdcTaskDecoratorTest {

  private MdcTaskDecorator decorator;

  /**
   * 测试目标：准备每个用例共用的装饰器和提交线程 MDC。
   *
   * 测试场景与前置条件：每个测试执行前调用。
   * 关键步骤：创建 MdcTaskDecorator，并写入 traceId=tid-abc。
   * 预期结果：被测装饰器可用，提交线程具有固定 MDC 值。
   */
  @BeforeEach
  void setUp() {
    decorator = new MdcTaskDecorator();
    MDC.put("traceId", "tid-abc");
  }

  /**
   * 测试目标：隔离用例间的线程上下文。
   *
   * 测试场景与前置条件：每个测试执行后调用。
   * 关键步骤：清空当前测试线程 MDC。
   * 预期结果：后续用例不会继承前一个用例的 traceId。
   */
  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  /** * 验证上下文复制；提交线程有traceId；异步执行装饰任务；预期工作线程读到同值. */
  @Test
  void testDecorateGivenMdcContextThenCopiedToWorkerThread() throws Exception {
    AtomicReference<String> seen = new AtomicReference<>();
    Runnable decorated = decorator.decorate(() -> seen.set(MDC.get("traceId")));

    Thread worker = new Thread(decorated);
    worker.start();
    worker.join();

    assertEquals("tid-abc", seen.get());
  }

  /** * 验证空上下文清理；提交线程无MDC；异步执行装饰任务；预期工作线程traceId为null. */
  @Test
  void testDecorateWhenNoSubmitContextThenWorkerMdcCleared() throws Exception {
    MDC.clear();
    AtomicReference<String> seen = new AtomicReference<>("keep");
    Runnable decorated = decorator.decorate(() -> seen.set(MDC.get("traceId")));

    Thread worker = new Thread(decorated);
    worker.start();
    worker.join();

    assertNull(seen.get());
  }

  /** * 验证原上下文恢复；工作线程已有traceId；执行装饰任务；预期返回后保留原值. */
  @Test
  void testDecorateThenRestoresWorkerOriginalContext() throws Exception {
    AtomicReference<String> after = new AtomicReference<>();
    Runnable decorated = decorator.decorate(() -> {});

    Thread worker =
        new Thread(
            () -> {
              MDC.put("traceId", "worker-tid");
              decorated.run();
              after.set(MDC.get("traceId"));
            });
    worker.start();
    worker.join();

    assertEquals("worker-tid", after.get());
  }
}
