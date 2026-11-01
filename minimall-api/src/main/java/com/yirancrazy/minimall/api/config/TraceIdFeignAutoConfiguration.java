package com.yirancrazy.minimall.api.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 自动注册 TraceId 与内部令牌 Feign 请求拦截器.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@AutoConfiguration
public class TraceIdFeignAutoConfiguration {

    /**
     * 创建从 MDC 读取链路标识的 Feign 拦截器.
     *
     * @return TraceId Feign 请求拦截器
     */
    @Bean
    @ConditionalOnMissingBean(name = "traceIdFeignInterceptor")
    public RequestInterceptor traceIdFeignInterceptor() {
        return new TraceIdFeignInterceptor();
    }

    /**
     * 创建从配置读取内部令牌的 Feign 拦截器.
     *
     * @param internalToken 服务间访问令牌
     * @return 内部令牌 Feign 请求拦截器
     */
    @Bean
    @ConditionalOnMissingBean(name = "internalTokenFeignInterceptor")
    public RequestInterceptor internalTokenFeignInterceptor(
            @Value("${minimall.security.internal-token:}") String internalToken) {
        return new InternalTokenFeignInterceptor(internalToken);
    }
}
