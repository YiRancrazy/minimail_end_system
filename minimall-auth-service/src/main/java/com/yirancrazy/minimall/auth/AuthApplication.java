package com.yirancrazy.minimall.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
* 认证服务 Spring Boot 启动入口，负责启动服务发现、Mapper 扫描与 JWT 配置装载。
 */
@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
@MapperScan("com.yirancrazy.minimall.auth.mapper")
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}