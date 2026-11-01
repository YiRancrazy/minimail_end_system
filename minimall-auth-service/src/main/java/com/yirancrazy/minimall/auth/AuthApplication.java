package com.yirancrazy.minimall.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/* 认证服务启动入口，负责装配本服务的基础设施组件。 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户、商家和平台账号认证服务入口；启用认证 Mapper 扫描与 API 模块 Feign 客户端。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.auth.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class AuthApplication {
    /**
     * 启动认证服务并初始化 Spring 容器。
     *
     * @param args Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
