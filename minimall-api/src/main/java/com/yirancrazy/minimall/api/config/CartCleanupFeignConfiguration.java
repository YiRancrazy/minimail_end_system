package com.yirancrazy.minimall.api.config;

import feign.Request;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车清理调用专用的短超时配置，避免拖慢订单主链路.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Configuration(proxyBeanMethods = false)
public class CartCleanupFeignConfiguration {

    /**
     * 创建连接和读取均限制为 500 毫秒的 Feign 请求选项.
     *
     * @return 购物车清理专用 Feign 请求选项
     */
    @Bean
    public Request.Options cartCleanupRequestOptions() {
        return new Request.Options(500, TimeUnit.MILLISECONDS, 500, TimeUnit.MILLISECONDS, true);
    }
}
