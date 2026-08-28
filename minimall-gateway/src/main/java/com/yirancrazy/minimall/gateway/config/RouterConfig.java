package com.yirancrazy.minimall.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Router配置类，配置Router相关 Bean
 * @Version: 1.1
 * @DateTime: 2026/08/04
 */
@Configuration
public class RouterConfig {

    /**
     * Direct-host URIs instead of {@code lb://} (Nacos service discovery)
     * because backend instances register on a non-loopback link-local address
     * ({@code 169.254.x.x}) that the gateway cannot reach.
     */
    private static final String HOST = "http://127.0.0.1";

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder b) {
        return b.routes()
            .route("id-service", r -> r.path("/api/v1/id/**")
                .uri(HOST + ":8211"))
            .route("auth-service", r -> r.path(
                "/api/v1/user/auth/**",
                "/api/v1/merchant/auth/**",
                "/api/v1/platform/auth/**")
                .uri(HOST + ":8201"))
            .route("user-service", r -> r.path(
                "/api/v1/user/users/**",
                "/api/v1/user/addresses/**",
                "/api/v1/user/favorites/**",
                "/api/v1/user/upload/**")
                .uri(HOST + ":8202"))
            .route("merchant-service", r -> r.path(
                "/api/v1/merchant/merchants/**",
                "/api/v1/merchant/shops/**")
                .uri(HOST + ":8203"))
            .route("goods-service", r -> r.path(
                "/api/v1/user/goods/**",
                "/api/v1/merchant/goods/**",
                "/api/v1/platform/goods/**",
                "/api/v1/merchant/upload/**")
                .uri(HOST + ":8204"))
            .route("cart-service", r -> r.path("/api/v1/user/cart/**")
                .uri(HOST + ":8205"))
            .route("order-service", r -> r.path(
                "/api/v1/user/orders/**",
                "/api/v1/merchant/orders/**",
                "/api/v1/platform/orders/**",
                "/api/v1/merchant/refunds/**")
                .uri(HOST + ":8206"))
            .route("pay-service", r -> r.path(
                "/api/v1/user/pay/**",
                "/api/v1/merchant/pay/**",
                "/api/v1/platform/pay/**",
                "/api/v1/pay/success")
                .uri(HOST + ":8207"))
            .route("stock-service", r -> r.path(
                "/api/v1/merchant/stock/**",
                "/api/v1/platform/stock/**")
                .uri(HOST + ":8208"))
            .route("notify-service", r -> r.path(
                "/api/v1/user/notify/**",
                "/api/v1/merchant/notify/**",
                "/api/v1/platform/notify/**",
                "/api/v1/platform/complaints/**",
                "/api/v1/user/comments/**",
                "/api/v1/merchant/comments/**",
                "/api/v1/platform/comments/**",
                "/api/v1/goods/comments/**",
                "/api/v1/notify/sse")
                .uri(HOST + ":8209"))
            .route("platform-service", r -> r.path(
                "/api/v1/platform/roles/**",
                "/api/v1/platform/merchants/**",
                "/api/v1/platform/users/**",
                "/api/v1/platform/health")
                .uri(HOST + ":8210"))
            .build();
    }
}