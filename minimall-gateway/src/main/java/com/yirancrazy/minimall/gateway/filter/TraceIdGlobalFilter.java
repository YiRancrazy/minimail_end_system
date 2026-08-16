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
 * @Description: TraceIdGlobal过滤器，处理TraceIdGlobal相关请求
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    // 全链路追踪ID（网关生成，服务间透传，用于日志归集与故障定界）
    public static final String HEADER = "X-Trace-Id";

    /**
     * 为每个请求生成或透传 traceId，便于分布式链路追踪与日志聚合。
     *
     * @param exchange 当前请求与响应交换上下文
     * @param chain 网关过滤器链，用于将携带 traceId 的请求传递给下游
     * @return 链路执行结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tid = sanitize(exchange.getRequest().getHeaders().getFirst(HEADER));
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header(HEADER, tid).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /**
     * 规整客户端传入的 traceId：剔除 CR/LF 控制字符（防日志伪造），超过 64 字符时截断；为空时生成 UUID。
     * @param raw 请求头原始值，可为 null
     * @return 规整后的 traceId
     */
    static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        String cleaned = raw.replace("\r", "").replace("\n", "");
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }

    /**
     * 返回过滤器执行顺序，确保 traceId 在最早期注入到请求中。
     *
     * @return 排序值，固定为 -100
     */
    @Override
    public int getOrder() {
        return -100;
    }
}