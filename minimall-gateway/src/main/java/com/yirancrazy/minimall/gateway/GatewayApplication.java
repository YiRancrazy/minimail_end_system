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
     * 规避 Spring Boot 3.x 回归问题（issue #49693）：
     * NettyReactiveWebServerAutoConfiguration 缺少
     * {@code @ConditionalOnWebApplication(type = REACTIVE)}，在部分 classpath
     * 下无法注册 {@link ReactiveWebServerFactory} Bean。
     * 显式声明该 Bean，确保无论自动配置是否生效，Reactor Netty 都能启动。
     */
    @Bean
    public ReactiveWebServerFactory nettyReactiveWebServerFactory() {
        return new org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory();
    }
}
