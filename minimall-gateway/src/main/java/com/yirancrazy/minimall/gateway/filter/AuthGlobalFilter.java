package com.yirancrazy.minimall.gateway.filter;

import com.yirancrazy.minimall.gateway.config.JwtVerifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Iter-1 JWT global filter. White-list login/register routes; for everything else
 * parse `Authorization: Bearer xxx` and forward userId / role as X-User-Id /
 * X-User-Role headers. On failure respond 401. Inline-validates JWT signature
 * with the same secret the auth-service uses.
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Pattern JWT_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    private static final Set<String> WHITELIST = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register"
    );

    private final JwtVerifier verifier;

    public AuthGlobalFilter(JwtVerifier verifier) {
        this.verifier = verifier;
    }

    /**
     * 网关 JWT 认证全局过滤器，校验令牌后向下游透传用户身份信息。
     *
     * @param exchange 当前请求与响应交换上下文
     * @param chain 网关过滤器链，用于将请求转交给后续过滤器或目标路由
     * @return 链路执行结果；白名单或验签失败时直接返回 401 JSON 响应
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (WHITELIST.contains(path) || path.startsWith("/internal/")) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(exchange, "missing token");
        }
        String token = auth.substring(7);
        if (!JWT_PATTERN.matcher(token).matches()) {
            return reject(exchange, "malformed token");
        }

        try {
            var claims = verifier.verify(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role == null ? "USER" : role)
                .header("X-Trace-Id",
                    exchange.getRequest().getHeaders().getFirst("X-Trace-Id") == null
                        ? UUID.randomUUID().toString().replace("-", "")
                        : exchange.getRequest().getHeaders().getFirst("X-Trace-Id"))
                .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception ex) {
            return reject(exchange, ex.getMessage());
        }
    }

    private Mono<Void> reject(ServerWebExchange exchange, String reason) {
        ServerHttpResponse res = exchange.getResponse();
        res.setStatusCode(HttpStatus.UNAUTHORIZED);
        res.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":\"14003\",\"message\":\"Token 无效: " + reason + "\"}";
        return res.writeWith(Mono.just(res.bufferFactory().wrap(
            body.getBytes(StandardCharsets.UTF_8))));
    }

    /**
     * 返回过滤器执行顺序，使其在 traceId 注入之后、其他业务过滤器之前执行。
     *
     * @return 排序值，固定为 -50
     */
    @Override
    public int getOrder() {
        return -50;
    }
}