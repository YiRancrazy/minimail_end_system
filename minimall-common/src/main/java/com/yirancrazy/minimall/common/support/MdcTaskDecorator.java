package com.yirancrazy.minimall.common.support;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 将任务提交线程的 MDC 上下文复制到异步执行线程，并在任务结束后恢复执行线程原有上下文。
 * 用于线程池复用场景，避免 traceId 丢失或串到其他任务。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class MdcTaskDecorator implements TaskDecorator {

  /**
   * 包装原始任务：执行前应用提交线程的 MDC 快照，执行后恢复工作线程原有 MDC。
   *
   * @param runnable 在线程池中执行的原始任务
   * @return 带 MDC 上下文切换和恢复逻辑的任务
   */
  @Override
  public Runnable decorate(Runnable runnable) {
    Map<String, String> submitContext = MDC.getCopyOfContextMap();
    return () -> {
      Map<String, String> previous = MDC.getCopyOfContextMap();
      try {
        if (submitContext == null) {
          MDC.clear();
        } else {
          MDC.setContextMap(submitContext);
        }
        runnable.run();
      } finally {
        if (previous == null) {
          MDC.clear();
        } else {
          MDC.setContextMap(previous);
        }
      }
    };
  }
}
