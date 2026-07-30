package com.yirancrazy.minimall.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import com.yirancrazy.minimall.gateway.config.JwtVerifier;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthGlobalFilter 类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Pattern JWT_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    private static final Set<String> WHITELIST = Set.of(
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh-token",
        "/actuator/health"
    );

    private static final Map<String, Set<String>> ROLE_PATHS = Map.of(
        "/api/v1/merchant", Set.of("MERCHANT", "PLATFORM"),
        "/api/v1/platform", Set.of("PLATFORM")
    );

    private static final Set<String> ALL_ROLES = Set.of("USER", "MERCHANT", "PLATFORM");

    private final JwtVerifier verifier;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthGlobalFilter(JwtVerifier verifier, ReactiveStringRedisTemplate redisTemplate) {
        this.verifier = verifier;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (WHITELIST.contains(path) || path.startsWith("/internal/")) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "missing token");
        }
        String token = auth.substring(7);
        if (!JWT_PATTERN.matcher(token).matches()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "malformed token");
        }

        Claims claims;
        try {
            claims = verifier.verify(token);
        }
        catch (JwtException ex) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "token invalid");
        }

        String jti = claims.get("jti", String.class);
        String role = claims.get("role", String.class);
        if (role == null) {
            role = "USER";
        }

        if (!isAuthorized(path, role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, "access denied");
        }

        if (jti != null) {
            return redisTemplate.hasKey("blacklist:jti:" + jti)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return reject(exchange, HttpStatus.UNAUTHORIZED, "token revoked");
                    }
                    return chain.filter(mutateWithHeaders(exchange, claims, jti));
                });
        }

        return chain.filter(mutateWithHeaders(exchange, claims, jti));
    }

    private boolean isAuthorized(String path, String role) {
        for (Map.Entry<String, Set<String>> entry : ROLE_PATHS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue().contains(role);
            }
        }
        return ALL_ROLES.contains(role);
    }

    private ServerWebExchange mutateWithHeaders(ServerWebExchange exchange, Claims claims, String jti) {
        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-User-Id", userId)
            .header("X-User-Role", role == null ? "USER" : role)
            .header("X-User-Jti", jti == null ? "" : jti)
            .header("X-Trace-Id",
                exchange.getRequest().getHeaders().getFirst("X-Trace-Id") == null
                    ? UUID.randomUUID().toString().replace("-", "")
                    : exchange.getRequest().getHeaders().getFirst("X-Trace-Id"))
            .build();
        return exchange.mutate().request(mutated).build();
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String reason) {
        ServerHttpResponse res = exchange.getResponse();
        res.setStatusCode(status);
        res.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String code = status == HttpStatus.FORBIDDEN ? "14007" : "14003";
        String body = "{\"code\":\"" + code + "\",\"message\":\"" + reason + "\"}";
        return res.writeWith(Mono.just(res.bufferFactory().wrap(
            body.getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        return -50;
    }
}