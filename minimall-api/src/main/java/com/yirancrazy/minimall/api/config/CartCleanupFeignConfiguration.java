package com.yirancrazy.minimall.api.config;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.Request;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车清理调用专用的短超时配置。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Configuration(proxyBeanMethods = false)
public class CartCleanupFeignConfiguration {

    /**
     * 创建购物车清理请求超时配置。
     * @return Feign 请求选项
     */
    @Bean
    public Request.Options cartCleanupRequestOptions() {
        return new Request.Options(
            500, TimeUnit.MILLISECONDS,
            500, TimeUnit.MILLISECONDS,
            true);
    }
}
