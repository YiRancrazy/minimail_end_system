package com.yirancrazy.minimall.id;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * 运行时命令行参数会原样传给 Spring Boot，节点编号由应用配置或环境变量提供。
 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: minimall-id-service 的 Spring Boot 启动入口。负责扫描 ID Mapper 并初始化
 *     内部发号请求使用的应用上下文，具体 ID 生成由 IdService 实现。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.id.mapper")
public class IdApplication {
  /**
   * 启动 Spring Boot 应用，触发组件扫描、Mapper 注册和 ID 服务依赖初始化。
   *
   * @param args 传给 Spring Boot 的命令行参数
   */
  public static void main(String[] args) {
    SpringApplication.run(IdApplication.class, args);
  }
}
