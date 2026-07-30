package com.yirancrazy.minimall.cart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
* 购物车服务 Spring Boot 启动入口，开启服务注册发现并扫描 cart 模块下的 MyBatis Mapper。
 */
@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
@MapperScan("com.yirancrazy.minimall.cart.mapper")
public class CartApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }
}