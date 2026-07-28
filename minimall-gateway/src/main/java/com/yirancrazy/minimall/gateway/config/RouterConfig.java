package com.yirancrazy.minimall.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouterConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder b) {
        return b.routes()
            .route("id-service", r -> r.path("/api/v1/id/**")
                .uri("lb://minimall-id-service"))
            .route("auth-service", r -> r.path("/api/v1/auth/**")
                .uri("lb://minimall-auth-service"))
            .build();
    }
}