package com.yirancrazy.minimall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import reactor.netty.http.server.HttpServer;


        /**
         * 应用启动入口。
         * @param args args 参数
         */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: GatewayApplication 服务启动类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {
    /**
    * 应用启动入口。
    * @param args 启动参数
    */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }


    /**
     * 执行 nettyReactiveWebServerFactory 方法。
     * @return 处理结果
     */
    @Bean
    public ReactiveWebServerFactory nettyReactiveWebServerFactory() {
        return new org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory();
    }
}
