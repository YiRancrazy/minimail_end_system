package com.yirancrazy.minimall.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RouterConfig description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class RouterConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder b) {
        return b.routes()
            .route("id-service", r -> r.path("/api/v1/id/**")
                .uri("lb://minimall-id-service"))
            .route("auth-service", r -> r.path("/api/v1/auth/**")
                .uri("lb://minimall-auth-service"))
            .route("user-service", r -> r.path("/api/v1/user/**")
                .uri("lb://minimall-user-service"))
            .route("merchant-service", r -> r.path("/api/v1/merchant/**")
                .uri("lb://minimall-merchant-service"))
            .route("goods-service", r -> r.path("/api/v1/goods/**")
                .uri("lb://minimall-goods-service"))
            .route("cart-service", r -> r.path("/api/v1/cart/**")
                .uri("lb://minimall-cart-service"))
            .build();
    }
}