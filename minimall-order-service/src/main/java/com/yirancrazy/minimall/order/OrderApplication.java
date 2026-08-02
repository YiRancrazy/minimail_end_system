package com.yirancrazy.minimall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单服务启动类，初始化 Spring Boot 应用上下文
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.order.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}