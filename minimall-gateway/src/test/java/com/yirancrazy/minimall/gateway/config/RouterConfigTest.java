package com.yirancrazy.minimall.gateway.config;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.support.GenericApplicationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RouterConfig 的单元测试类，校验网关路由走服务发现且不暴露内部 ID 服务。
 * @Version: 1.0
 * @DateTime: 2026/09/23
 **/
class RouterConfigTest {

    private GenericApplicationContext context;

    private RouteLocator routeLocator;

    @BeforeEach
    void setUp() {
        context = new GenericApplicationContext();
        context.registerBean(PathRoutePredicateFactory.class);
        context.refresh();
        routeLocator = new RouterConfig().routeLocator(new RouteLocatorBuilder(context));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void routes_target_backends_through_service_discovery() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
        assertEquals(10, routes.size());
        for (Route route : routes) {
            assertEquals("lb://minimall-" + route.getId(), route.getUri().toString(),
                "route " + route.getId() + " must be load balanced");
        }
    }

    @Test
    void internal_id_service_is_not_exposed() {
        List<String> routeIds = routeLocator.getRoutes().map(Route::getId).collectList().block();
        assertNotNull(routeIds);
        assertFalse(routeIds.contains("id-service"), "internal id service must not be routed: " + routeIds);
    }
}
