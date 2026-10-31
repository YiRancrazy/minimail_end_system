package com.yirancrazy.minimall.common.config;

import com.yirancrazy.minimall.common.support.MdcTaskDecorator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 注册将提交线程 MDC 上下文复制到工作线程的 TaskDecorator，
 * 用于线程池异步任务继续关联同一 traceId；已有同类 Bean 时不覆盖。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@AutoConfiguration
public class TraceIdPropagationAutoConfiguration {

  /**
   * 创建 MDC 上下文传播装饰器。
   *
   * @return 可包装异步 Runnable 的 MDC 装饰器
   */
  @Bean
  @ConditionalOnMissingBean
  public TaskDecorator mdcTaskDecorator() {
    return new MdcTaskDecorator();
  }
}
