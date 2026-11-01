package com.yirancrazy.minimall.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 声明网关静态路由表，通过负载均衡服务发现 URI 将公共 API 路径族映射到后端服务。
 *   本类仅负责路由匹配和服务归属，鉴权与请求加固由全局过滤器执行。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Configuration
public class RouterConfig {

    /**
     * 构建用于转发公共 API 流量的 11 条网关路由。
     *
     * <p>多数服务仅使用路径匹配。平台导出端点使用按 HTTP 方法区分的路由：POST 导出请求转发到
     * platform-service，其他方法继续转发到 order-service 或 pay-service。路径谓词重叠时，路由声明
     * 顺序会影响匹配结果。
     *
     * @param b Spring 提供并绑定网关应用上下文的路由构建器
     * @return 供 Spring Cloud Gateway 使用的路由定位器
     */
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder b) {
        return b.routes()
                .route(
                        "auth-service",
                        r ->
                                r.path(
                                                "/api/v1/user/auth/**",
                                                "/api/v1/merchant/auth/**",
                                                "/api/v1/platform/auth/**")
                                        .uri("lb://minimall-auth-service"))
                .route(
                        "user-service",
                        r ->
                                r.path(
                                                "/api/v1/user/users/**",
                                                "/api/v1/user/addresses/**",
                                                "/api/v1/user/favorites/**",
                                                "/api/v1/user/upload/**")
                                        .uri("lb://minimall-user-service"))
                .route(
                        "merchant-service",
                        r ->
                                r.path("/api/v1/merchant/merchants/**", "/api/v1/merchant/shops/**")
                                        .uri("lb://minimall-merchant-service"))
                .route(
                        "goods-service",
                        r ->
                                r.path(
                                                "/api/v1/user/goods/**",
                                                "/api/v1/merchant/goods/**",
                                                "/api/v1/platform/goods/**",
                                                "/api/v1/merchant/upload/**")
                                        .uri("lb://minimall-goods-service"))
                .route(
                        "cart-service", r -> r.path("/api/v1/user/cart/**").uri("lb://minimall-cart-service"))
                .route(
                        "platform-summary-service",
                        r ->
                                r.path(
                                                "/api/v1/platform/orders/export",
                                                "/api/v1/platform/pay/reconciliation/export")
                                        .and()
                                        .method(HttpMethod.POST)
                                        .uri("lb://minimall-platform-service"))
                .route(
                        "order-service",
                        r ->
                                r.path(
                                                "/api/v1/user/orders/**",
                                                "/api/v1/merchant/orders/**",
                                                "/api/v1/platform/orders/**",
                                                "/api/v1/merchant/refunds/**")
                                        .uri("lb://minimall-order-service"))
                .route(
                        "pay-service",
                        r ->
                                r.path(
                                                "/api/v1/user/pay/**",
                                                "/api/v1/merchant/pay/**",
                                                "/api/v1/platform/pay/**",
                                                "/api/v1/pay/success")
                                        .uri("lb://minimall-pay-service"))
                .route(
                        "stock-service",
                        r ->
                                r.path("/api/v1/merchant/stock/**", "/api/v1/platform/stock/**")
                                        .uri("lb://minimall-stock-service"))
                .route(
                        "notify-service",
                        r ->
                                r.path(
                                                "/api/v1/user/notify/**",
                                                "/api/v1/merchant/notify/**",
                                                "/api/v1/platform/notify/**",
                                                "/api/v1/platform/complaints/**",
                                                "/api/v1/user/preferences/**",
                                                "/api/v1/user/complaints/**",
                                                "/api/v1/user/comments/**",
                                                "/api/v1/merchant/comments/**",
                                                "/api/v1/platform/comments/**",
                                                "/api/v1/goods/comments/**",
                                                "/api/v1/notify/sse")
                                        .uri("lb://minimall-notify-service"))
                .route(
                        "platform-service",
                        r ->
                                r.path(
                                                "/api/v1/platform/roles/**",
                                                "/api/v1/platform/merchants/**",
                                                "/api/v1/platform/users/**",
                                                "/api/v1/platform/health")
                                        .uri("lb://minimall-platform-service"))
                .build();
    }
}
