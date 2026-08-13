package com.yirancrazy.minimall.common.filter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 注册 {@link TraceIdFilter}，使各服务无论是否经过网关都能注入并透传 traceId。
 *               仅 servlet Web 应用生效（网关为响应式项目，自带 TraceIdGlobalFilter）。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TraceIdFilterAutoConfiguration {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        // 尽早执行，使后续过滤器/拦截器与业务代码都能读取到 MDC traceId
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
