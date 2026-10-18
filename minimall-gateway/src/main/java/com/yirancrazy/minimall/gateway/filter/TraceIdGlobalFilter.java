package com.yirancrazy.minimall.gateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: TraceIdGlobalFilter 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    // 全链路追踪ID（网关生成，服务间透传，用于日志归集与故障定界）
    public static final String HEADER = "X-Trace-Id";


    /**
     * 执行 filter 方法。
     * @param exchange exchange 参数
     * @param chain chain 参数
     * @return 处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tid = sanitize(exchange.getRequest().getHeaders().getFirst(HEADER));
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header(HEADER, tid).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }


    /**
     * 处理sanitize。
     * @param raw raw
     * @return 处理结果
     */
    static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        String cleaned = raw.replace("\r", "").replace("\n", "");
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }


    /**
     * 获取相关数据。
     * @return 业务数值
     */
    @Override
    public int getOrder() {
        return -100;
    }
}