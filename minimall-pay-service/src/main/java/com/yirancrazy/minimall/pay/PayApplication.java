package com.yirancrazy.minimall.pay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * 支付服务独立进程入口，启用 Mapper、Feign 客户端和定时调度。
 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付服务独立进程的启动入口。
 * 负责扫描支付模块 Mapper、装配业务 Feign 客户端并开启定时调度；仅用于启动支付服务。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.pay.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
@EnableScheduling
public class PayApplication {
  /**
   * 启动支付服务 Spring Boot 应用。
   *
   * @param args 命令行启动参数
   */
  public static void main(String[] args) {
    SpringApplication.run(PayApplication.class, args);
  }
}
