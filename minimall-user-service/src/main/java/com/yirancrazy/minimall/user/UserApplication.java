package com.yirancrazy.minimall.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户服务启动入口，负责扫描本模块 Mapper 和 minimall-api 下的 Feign 客户端。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.user.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class UserApplication {
  /**
   * 启动用户服务 Spring 容器。
   *
   * @param args 命令行启动参数，可为空
   */
  public static void main(String[] args) {
    SpringApplication.run(UserApplication.class, args);
  }
}
