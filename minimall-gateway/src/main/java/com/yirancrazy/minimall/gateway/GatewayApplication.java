package com.yirancrazy.minimall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import reactor.netty.http.server.HttpServer;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 网关服务启动类，初始化 Spring Boot 应用上下文
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    /**
     * Workaround for Spring Boot 3.x regression (issue #49693):
     * NettyReactiveWebServerAutoConfiguration is missing
     * {@code @ConditionalOnWebApplication(type = REACTIVE)} and, on some
     * classpaths, fails to register a {@link ReactiveWebServerFactory} bean.
     * Declaring one explicitly guarantees Reactor Netty starts regardless of
     * whether the auto-configuration kicks in.
     */
    @Bean
    public ReactiveWebServerFactory nettyReactiveWebServerFactory() {
        return new org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory();
    }
}