package com.yirancrazy.minimall.stock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * 加载库存服务自动配置、Mapper 扫描和外部中间件配置。
 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存微服务启动入口，负责初始化 Spring 上下文并扫描库存 Mapper。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.stock.mapper")
public class StockApplication {
  /**
   * 启动库存微服务并等待外部组件完成 Bean 初始化。
   *
   * @param args Spring Boot 命令行参数
   */
  public static void main(String[] args) {
    SpringApplication.run(StockApplication.class, args);
  }
}
