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
 * @Description: 规范化并在网关内透传 X-Trace-Id 请求头。复用安全的入站标识，或在缺失时生成
 *   32 字符 UUID，然后改写下游请求；不会将标识写入响应，也不执行鉴权。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    // 全链路追踪ID（网关生成，服务间透传，用于日志归集与故障定界）
    public static final String HEADER = "X-Trace-Id";

    /**
     * 清洗或生成链路标识，并通过改写后的下游请求继续传递。
     *
     * @param exchange 当前请求和响应交换对象
     * @param chain 剩余网关过滤器链
     * @return 剩余过滤器链的完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tid = sanitize(exchange.getRequest().getHeaders().getFirst(HEADER));
        ServerHttpRequest mutated = exchange.getRequest().mutate().header(HEADER, tid).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /**
     * 返回可安全传递到下游的链路标识。
     *
     * <p>空值替换为去除连字符的 UUID。已有值会移除 CR/LF，并截断到 64 字符；其他内容不做校验。
     *
     * @param raw 不可信的入站 X-Trace-Id 值
     * @return 新生成或清洗后的链路标识
     */
    static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        String cleaned = raw.replace("\r", "").replace("\n", "");
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }

    /**
     * 将链路标识透传安排在鉴权及其他网关过滤器之前。
     *
     * @return 过滤器顺序 -100
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
