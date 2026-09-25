package com.yirancrazy.minimall.gateway.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.handler.predicate.MethodRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 网关路由上下文测试，校验 RouterConfig 在 Spring 上下文中装配出可用的路由定位器。
 * @Version: 1.0
 * @DateTime: 2026/09/23
 **/
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RouterConfig.class, GatewayRouteContextTest.TestGatewayConfig.class})
class GatewayRouteContextTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void context_assembles_route_locator() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
        assertEquals(11, routes.size());
        assertTrue(routes.stream().allMatch(route -> route.getUri().getScheme().equals("lb")),
            "every route must be resolved by the load balancer: " + routes);
    }

    /**
     * Minimal wiring that mirrors the beans GatewayAutoConfiguration contributes to RouterConfig.
     */
    @Configuration
    static class TestGatewayConfig {

        /**
         * 构造路由定位器构建器。
         * @param context 当前应用上下文
         * @return 路由定位器构建器
         */
        @Bean
        RouteLocatorBuilder routeLocatorBuilder(ConfigurableApplicationContext context) {
            return new RouteLocatorBuilder(context);
        }

        /**
         * 构造路径断言工厂，供 lb 路由的 path 断言使用。
         * @return 路径断言工厂
         */
        @Bean
        PathRoutePredicateFactory pathRoutePredicateFactory() {
            return new PathRoutePredicateFactory();
        }

        /**
         * 构造 HTTP 方法断言工厂，供平台导出路由的 POST 约束使用。
         * @return HTTP 方法断言工厂
         */
        @Bean
        MethodRoutePredicateFactory methodRoutePredicateFactory() {
            return new MethodRoutePredicateFactory();
        }
    }
}
