package com.yirancrazy.minimall.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import feign.RequestInterceptor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: TraceIdFeignAutoConfiguration 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@AutoConfiguration
public class TraceIdFeignAutoConfiguration {

    /**
     * 执行 traceIdFeignInterceptor 方法。
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(name = "traceIdFeignInterceptor")
    public RequestInterceptor traceIdFeignInterceptor() {
        return new TraceIdFeignInterceptor();
    }


    /**
     * 创建内部令牌 Feign 拦截器。
     * @param internalToken 服务间访问令牌
     * @return Feign 请求拦截器
     */
    @Bean
    @ConditionalOnMissingBean(name = "internalTokenFeignInterceptor")
    public RequestInterceptor internalTokenFeignInterceptor(
            @Value("${minimall.security.internal-token:}") String internalToken) {
        return new InternalTokenFeignInterceptor(internalToken);
    }
}
