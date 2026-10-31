package com.yirancrazy.minimall.cart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/*
 * 通过注解启用 Mapper 扫描与 Feign 客户端发现，并启动购物车服务.
 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: 启动购物车服务，同时启用 MyBatis Mapper 扫描和共享 Feign 客户端包。该类不承载
 *     购物车业务决策，嵌入式服务器和应用依赖初始化完成后才进入请求处理阶段。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.cart.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class CartApplication {
  /**
   * 使用传入的 Spring Boot 参数启动购物车服务.
   *
   * @param args 原样转发给 SpringApplication 的命令行参数
   */
  public static void main(String[] args) {
    SpringApplication.run(CartApplication.class, args);
  }
}
