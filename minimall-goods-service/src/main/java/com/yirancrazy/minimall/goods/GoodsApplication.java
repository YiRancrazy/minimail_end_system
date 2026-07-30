package com.yirancrazy.minimall.goods;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
* 商品服务 Spring Boot 启动入口，负责商品模块的初始化与注册中心接入。
 */
@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
@MapperScan("com.yirancrazy.minimall.goods.mapper")
public class GoodsApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoodsApplication.class, args);
    }
}