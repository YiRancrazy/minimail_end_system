package com.yirancrazy.minimall.api.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import feign.RequestInterceptor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Feign 请求拦截器自动装配，向使用 OpenFeign 的服务提供 traceId 跨服务透传能力。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 */
@AutoConfiguration
public class TraceIdFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestInterceptor traceIdFeignInterceptor() {
        return new TraceIdFeignInterceptor();
    }
}
