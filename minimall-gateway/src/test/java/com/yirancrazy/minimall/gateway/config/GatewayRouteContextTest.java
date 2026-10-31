package com.yirancrazy.minimall.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 RouterConfig 能否在最小 Spring 上下文中装配完整路由定位器。测试隔离完整网关
 *   应用，仅提供编译路由定义所需的谓词工厂。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {RouterConfig.class, GatewayRouteContextTest.TestGatewayConfig.class})
class GatewayRouteContextTest {

  @Autowired private RouteLocator routeLocator;

  /** 验证最小 Spring 上下文装配 11 条负载均衡路由；前置为注册路由配置与断言工厂；执行加载定位器并收集路由；预期 URI 均使用 lb. */
  @Test
  void testContextAssemblesRouteLocator() {
    List<Route> routes = routeLocator.getRoutes().collectList().block();
    assertNotNull(routes);
    assertEquals(11, routes.size());
    assertTrue(
        routes.stream().allMatch(route -> route.getUri().getScheme().equals("lb")),
        "every route must be resolved by the load balancer: " + routes);
  }

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 为当前测试上下文提供 RouterConfig 所需的路由构建器、路径谓词工厂和方法谓词
   *   工厂；有意排除生产运行时组件。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  @Configuration
  static class TestGatewayConfig {

    /**
     * 创建绑定测试应用上下文的路由构建器。
     *
     * @param context 用于解析路由组件的测试应用上下文
     * @return 与 RouterConfig 共享的路由构建器
     */
    @Bean
    RouteLocatorBuilder routeLocatorBuilder(ConfigurableApplicationContext context) {
      return new RouteLocatorBuilder(context);
    }

    /**
     * 提供 RouterConfig 所需的路径谓词工厂。
     *
     * @return 测试上下文使用的路径谓词工厂
     */
    @Bean
    PathRoutePredicateFactory pathRoutePredicateFactory() {
      return new PathRoutePredicateFactory();
    }

    /**
     * 提供导出路由所需的 HTTP 方法谓词工厂。
     *
     * @return 测试上下文使用方法谓词工厂
     */
    @Bean
    MethodRoutePredicateFactory methodRoutePredicateFactory() {
      return new MethodRoutePredicateFactory();
    }
  }
}
