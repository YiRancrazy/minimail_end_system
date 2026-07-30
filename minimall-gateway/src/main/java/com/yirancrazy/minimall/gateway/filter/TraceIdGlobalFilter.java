package com.yirancrazy.minimall.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
* 网关链路追踪全局过滤器，为每个请求生成/透传 traceId。
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

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
        String tid = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (tid == null || tid.isBlank()) {
            tid = UUID.randomUUID().toString().replace("-", "");
        }
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header(HEADER, tid).build();
        return chain.filter(exchange.mutate().request(mutated).build());
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