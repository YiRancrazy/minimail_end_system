package com.yirancrazy.minimall.goods;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


        /**
         * 应用启动入口。
         * @param args args 参数
         */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsApplication 服务启动类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.goods.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class GoodsApplication {
    /**
    * 应用启动入口。
    * @param args 启动参数
    */
    public static void main(String[] args) {
        SpringApplication.run(GoodsApplication.class, args);
    }
}