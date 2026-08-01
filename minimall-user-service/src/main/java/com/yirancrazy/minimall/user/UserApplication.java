package com.yirancrazy.minimall.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户服务启动类，初始化 Spring Boot 应用上下文
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.user.mapper")
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}