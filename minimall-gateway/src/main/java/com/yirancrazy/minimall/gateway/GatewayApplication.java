package com.yirancrazy.minimall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

/*
 * 响应式网关进程。路由、鉴权和链路追踪分别由 RouterConfig、AuthGlobalFilter 和
 * TraceIdGlobalFilter 实现，本类仅负责启动这些组件所在的上下文。
 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: 启动 Spring Cloud Gateway 应用并启用服务发现。本类仅负责启动流程和响应式服务器
 *   装配，路由匹配、鉴权与链路追踪均委托给网关组件完成。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {
  /**
   * 启动 Spring Boot 应用上下文，并在响应式网关运行期间阻塞主线程。
   *
   * @param args 转发给 Spring Boot 的命令行参数
   */
  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }

  /**
   * 创建 Spring Cloud Gateway 运行时使用的 Netty 响应式 Web 服务器工厂。
   *
   * @return 生命周期由 Spring Boot 管理的 Netty 响应式 Web 服务器工厂
   */
  @Bean
  public ReactiveWebServerFactory nettyReactiveWebServerFactory() {
    return new org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory();
  }
}
