package com.yirancrazy.minimall.cart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车服务启动类，初始化 Spring Boot 应用上下文
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.cart.mapper")
public class CartApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}