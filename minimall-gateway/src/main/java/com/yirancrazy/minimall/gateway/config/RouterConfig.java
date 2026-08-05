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

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder b) {
        return b.routes()
            .route("id-service", r -> r.path("/api/v1/id/**")
                .uri("lb://minimall-id-service"))
            .route("auth-service", r -> r.path(
                "/api/v1/user/auth/**",
                "/api/v1/merchant/auth/**",
                "/api/v1/platform/auth/**")
                .uri("lb://minimall-auth-service"))
            .route("user-service", r -> r.path(
                "/api/v1/user/users/**",
                "/api/v1/user/addresses/**",
                "/api/v1/user/favorites/**")
                .uri("lb://minimall-user-service"))
            .route("merchant-service", r -> r.path(
                "/api/v1/merchant/merchants/**")
                .uri("lb://minimall-merchant-service"))
            .route("goods-service", r -> r.path(
                "/api/v1/user/goods/**",
                "/api/v1/merchant/goods/**",
                "/api/v1/platform/goods/**")
                .uri("lb://minimall-goods-service"))
            .route("cart-service", r -> r.path("/api/v1/user/cart/**")
                .uri("lb://minimall-cart-service"))
            .route("order-service", r -> r.path(
                "/api/v1/user/orders/**",
                "/api/v1/merchant/orders/**",
                "/api/v1/platform/orders/**")
                .uri("lb://minimall-order-service"))
            .route("pay-service", r -> r.path(
                "/api/v1/user/pay/**",
                "/api/v1/merchant/pay/**",
                "/api/v1/platform/pay/**")
                .uri("lb://minimall-pay-service"))
            .route("stock-service", r -> r.path(
                "/api/v1/merchant/stock/**",
                "/api/v1/platform/stock/**")
                .uri("lb://minimall-stock-service"))
            .route("notify-service", r -> r.path(
                "/api/v1/user/notify/**",
                "/api/v1/merchant/notify/**",
                "/api/v1/platform/notify/**",
                "/api/v1/platform/complaints/**")
                .uri("lb://minimall-notify-service"))
            .route("platform-service", r -> r.path(
                "/api/v1/platform/roles/**")
                .uri("lb://minimall-platform-service"))
            .build();
    }
}