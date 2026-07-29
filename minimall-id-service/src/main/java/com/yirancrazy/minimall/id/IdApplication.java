package com.yirancrazy.minimall.id;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ID 服务 Spring Boot 启动入口，负责引导容器加载并注册到 Nacos 注册中心。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
@MapperScan("com.yirancrazy.minimall.id.mapper")
public class IdApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdApplication.class, args);
    }
}