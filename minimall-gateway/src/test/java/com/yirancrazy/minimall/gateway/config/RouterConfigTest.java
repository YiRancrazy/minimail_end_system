package com.yirancrazy.minimall.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import reactor.core.publisher.Mono;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证静态网关路由表，包括服务发现目标、内部服务隔离、通知路径归属和按 HTTP 方法
 *   区分的导出路由。每个测试都会在最小 Spring 上下文中构建路由定位器。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class RouterConfigTest {

  private static final int EXPECTED_ROUTES = 11;

  private GenericApplicationContext context;

  private RouteLocator routeLocator;

  private List<Route> routes;

  /**
   * 创建最小路由上下文，并缓存各测试共享的路由列表。
   */
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

  /**
   * 释放测试应用上下文持有的资源。
   */
  @AfterEach
  void tearDown() {
    context.close();
  }

  /** 验证所有路由经服务发现转发；前置为最小上下文已生成路由；执行检查目标 URI；预期 11 条均使用 Minimall 负载均衡地址. */
  @Test
  void testRoutesTargetBackendsThroughServiceDiscovery() {
    assertEquals(EXPECTED_ROUTES, routes.size());
    for (Route route : routes) {
      assertTrue(
          route.getUri().toString().startsWith("lb://minimall-"),
          "route " + route.getId() + " must be load balanced: " + route.getUri());
    }
  }

  /** 验证内部 ID 服务不暴露；前置为生产路由已加载；执行收集路由标识；预期不存在 id-service 路由. */
  @Test
  void testInternalIdServiceIsNotExposed() {
    assertFalse(routeIds().contains("id-service"), "internal id service must not be routed");
  }

  /** 验证通知服务覆盖评论、偏好与投诉路径；前置为路由已加载；执行解析代表路径；预期均命中 notify-service. */
  @Test
  void testNotifyPathsCoveredByControllerMapping() {
    assertEquals("notify-service", routeIdForGet("/api/v1/goods/comments/1"));
    assertEquals("notify-service", routeIdForGet("/api/v1/user/preferences"));
    assertEquals("notify-service", routeIdForGet("/api/v1/user/complaints"));
  }

  /** 验证导出路径按方法分流；前置为摘要路由先于通用订单路由；执行解析 GET 与 POST；预期导出归摘要服务. */
  @Test
  void testPlatformOrderRoutesUseHttpMethodsAndUniqueOwners() {
    assertEquals("order-service", routeIdForGet("/api/v1/platform/orders/export"));
    assertEquals("platform-summary-service", routeIdForPost("/api/v1/platform/orders/export"));
    assertEquals("order-service", routeIdForGet("/api/v1/platform/orders/summary"));
    assertEquals(
        "platform-summary-service", routeIdForPost("/api/v1/platform/pay/reconciliation/export"));
  }

  /**
   * 按路由定位器暴露的顺序返回全部路由标识。
   *
   * @return 不可变路由标识列表
   */
  private List<String> routeIds() {
    return routes.stream().map(Route::getId).toList();
  }

  /**
   * 解析与模拟 GET 请求匹配的路由。
   *
   * @param path 请求路径
   * @return 首个匹配的路由标识；无匹配路由时返回 null
   */
  private String routeIdForGet(String path) {
    return routeIdFor(MockServerHttpRequest.get(path).build());
  }

  /**
   * 解析与模拟 POST 请求匹配的路由。
   *
   * @param path 请求路径
   * @return 首个匹配的路由标识；无匹配路由时返回 null
   */
  private String routeIdForPost(String path) {
    return routeIdFor(MockServerHttpRequest.post(path).build());
  }

  /**
   * 按声明顺序为模拟请求评估路由谓词。
   *
   * @param request 模拟 HTTP 请求
   * @return 首个匹配的路由标识；所有谓词均不匹配时返回 null
   */
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
