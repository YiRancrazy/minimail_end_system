package com.yirancrazy.minimall.api.config;

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
    @ConditionalOnMissingBean
    public RequestInterceptor traceIdFeignInterceptor() {
        return new TraceIdFeignInterceptor();
    }
}
