package com.yirancrazy.minimall.common.filter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 仅针对 Servlet Web 应用注册 TraceIdFilter，并将其顺序提前到多数业务过滤器之前。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TraceIdFilterAutoConfiguration {

    /**
     * 创建 traceId 过滤器注册信息并设置较高执行优先级。
     *
     * @return Servlet 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration =
                new FilterRegistrationBean<>(new TraceIdFilter());
        // 尽早执行，使后续过滤器/拦截器与业务代码都能读取到 MDC traceId
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
