package com.yirancrazy.minimall.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import com.yirancrazy.minimall.common.support.MdcTaskDecorator;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: traceId 全链路传播自动装配，注册 MDC 透传装饰器供 Spring 异步线程池使用。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 */
@AutoConfiguration
public class TraceIdPropagationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }
}
