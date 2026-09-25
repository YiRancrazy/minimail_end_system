package com.yirancrazy.minimall.gateway.config;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.handler.predicate.MethodRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import reactor.core.publisher.Mono;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RouterConfig 的单元测试类，校验网关路由走服务发现、路径归属正确且不暴露内部 ID 服务。
 * @Version: 2.1
 * @DateTime: 2026/09/25
 **/
class RouterConfigTest {

    private static final int EXPECTED_ROUTES = 11;

    private GenericApplicationContext context;

    private RouteLocator routeLocator;

    private List<Route> routes;

    @BeforeEach
    void setUp() {
        context = new GenericApplicationContext();
        context.registerBean(MethodRoutePredicateFactory.class);
        context.registerBean(PathRoutePredicateFactory.class);
        context.refresh();
        routeLocator = new RouterConfig().routeLocator(new RouteLocatorBuilder(context));
        routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void routes_target_backends_through_service_discovery() {
        assertEquals(EXPECTED_ROUTES, routes.size());
        for (Route route : routes) {
            assertTrue(route.getUri().toString().startsWith("lb://minimall-"),
                "route " + route.getId() + " must be load balanced: " + route.getUri());
        }
    }

    @Test
    void internal_id_service_is_not_exposed() {
        assertFalse(routeIds().contains("id-service"), "internal id service must not be routed");
    }

    @Test
    void notify_paths_covered_by_controller_mapping() {
        assertEquals("notify-service", routeIdForGet("/api/v1/goods/comments/1"));
        assertEquals("notify-service", routeIdForGet("/api/v1/user/preferences"));
        assertEquals("notify-service", routeIdForGet("/api/v1/user/complaints"));
    }

    @Test
    void platform_order_routes_use_http_methods_and_unique_owners() {
        assertEquals("order-service", routeIdForGet("/api/v1/platform/orders/export"));
        assertEquals("platform-summary-service", routeIdForPost("/api/v1/platform/orders/export"));
        assertEquals("order-service", routeIdForGet("/api/v1/platform/orders/summary"));
        assertEquals("platform-summary-service",
            routeIdForPost("/api/v1/platform/pay/reconciliation/export"));
    }

    private List<String> routeIds() {
        return routes.stream().map(Route::getId).toList();
    }

    private String routeIdForGet(String path) {
        return routeIdFor(MockServerHttpRequest.get(path).build());
    }

    private String routeIdForPost(String path) {
        return routeIdFor(MockServerHttpRequest.post(path).build());
    }

    private String routeIdFor(MockServerHttpRequest request) {
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        for (Route route : routes) {
            if (Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block())) {
                return route.getId();
            }
        }
        return null;
    }
}
