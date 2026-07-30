package com.yirancrazy.minimall.stock;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
* 库存服务 Spring Boot 启动入口。扫描 com.yirancrazy.minimall 下的组件，
 *               开启服务发现向 Nacos 注册实例，并注册 stock.mapper 包下的 MyBatis-Plus Mapper，
 *               对外提供 SKU 库存查询、预占与释放能力。
 */
@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
@MapperScan("com.yirancrazy.minimall.stock.mapper")
public class StockApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class, args);
    }
}