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
 * @Description: GatewayRouteContextTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
     * @Author: yirancrazy@gmail.com
     * @Description: TestGatewayConfig 配置类。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    @Configuration
    static class TestGatewayConfig {


        @Bean
        RouteLocatorBuilder routeLocatorBuilder(ConfigurableApplicationContext context) {
            return new RouteLocatorBuilder(context);
        }


        @Bean
        PathRoutePredicateFactory pathRoutePredicateFactory() {
            return new PathRoutePredicateFactory();
        }


        @Bean
        MethodRoutePredicateFactory methodRoutePredicateFactory() {
            return new MethodRoutePredicateFactory();
        }
    }
}
