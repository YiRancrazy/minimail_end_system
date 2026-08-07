package com.yirancrazy.minimall.id;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ID生成服务启动类，初始化 Spring Boot 应用上下文
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.id.mapper")
public class IdApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdApplication.class, args);
    }
}