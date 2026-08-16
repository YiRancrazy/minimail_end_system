package com.yirancrazy.minimall.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
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
 * @Description: AuthGlobalFilter，网关全局安全过滤器，负责JWT鉴权、内部接口隔离、写接口幂等键校验和敏感接口防重放。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Pattern JWT_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    private static final Set<String> WHITELIST = Set.of(
        "/api/v1/user/auth/login",
        "/api/v1/user/auth/register",
        "/api/v1/user/auth/token/refresh",
        "/api/v1/user/auth/password/reset/request",
        "/api/v1/user/auth/password/reset/confirm",
        "/api/v1/user/pay/callback/alipay",
        "/api/v1/pay/success",
        "/api/v1/merchant/auth/login",
        "/api/v1/merchant/auth/register",
        "/api/v1/platform/auth/login",
        "/actuator/health"
    );

    private static final Map<String, Set<String>> ROLE_PATHS = Map.of(
        "/api/v1/merchant", Set.of("MERCHANT", "PLATFORM"),
        "/api/v1/platform", Set.of("PLATFORM")
    );

    private static final Set<String> ALL_ROLES = Set.of("USER", "MERCHANT", "PLATFORM");

    private static final String ROLE_MERCHANT = "MERCHANT";

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final long TIMESTAMP_WINDOW_MS = 5 * 60 * 1000L;

    private static final Duration NONCE_TTL = Duration.ofMinutes(10);

    private static final int NONCE_MIN_LEN = 6;

    private static final int NONCE_MAX_LEN = 16;

    private static final Set<String> SENSITIVE_WRITE_PREFIXES = Set.of(
        "/api/v1/user/pay/",
        "/api/v1/merchant/pay/",
        "/api/v1/platform/pay/"
    );

    private final JwtVerifier verifier;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final String internalToken;

    /**
     * 构造函数，注入JWT验签器、Redis模板和内部接口令牌。
     *
     * @param verifier JWT验签器
     * @param redisTemplate 响应式Redis字符串模板
     * @param internalToken 内部接口共享令牌
     */
    public AuthGlobalFilter(JwtVerifier verifier,
                            ReactiveStringRedisTemplate redisTemplate,
                            @Value("${minimall.security.internal-token:}") String internalToken) {
        this.verifier = verifier;
        this.redisTemplate = redisTemplate;
        this.internalToken = internalToken;
    }

    /**
     * 全局认证过滤器，验证JWT令牌、隔离内部接口、校验写接口幂等键和敏感接口防重放。
     *
     * @param exchange 服务端交换上下文
     * @param chain 过滤器链
     * @return 过滤器执行结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        if (WHITELIST.contains(path)) {
            return chain.filter(exchange);
        }

        // 判断是否是内部请求链接
        if (path.startsWith("/internal/")) {
            return handleInternal(exchange, chain);
        }

        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "missing token", "14003");
        }
        String token = auth.substring(7);
        if (!JWT_PATTERN.matcher(token).matches()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "malformed token", "14003");
        }

        Claims claims;
        try {
            claims = verifier.verify(token);
        }
        catch (JwtException ex) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "token invalid", "14003");
        }

        String jti = claims.get("jti", String.class);
        String role = claims.get("role", String.class);
        if (role == null) {
            role = "USER";
        }

        if (!isAuthorized(path, role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, "access denied", "14007");
        }

        if (jti != null) {
            return redisTemplate.hasKey("blacklist:jti:" + jti)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return reject(exchange, HttpStatus.UNAUTHORIZED, "token revoked", "14003");
                    }
                    ServerWebExchange mutated = mutateWithHeaders(exchange, claims, jti);
                    return postAuthChecks(mutated, chain, path, method);
                });
        }

        ServerWebExchange mutated = mutateWithHeaders(exchange, claims, jti);
        return postAuthChecks(mutated, chain, path, method);
    }

    /**
     * 校验内部接口请求来源，要求携带正确的X-Internal-Token。
     *
     * @param exchange 服务端交换上下文
     * @param chain 过滤器链
     * @return 过滤器执行结果
     */
    private Mono<Void> handleInternal(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求中携带的 X-Internal-Token
        String token = exchange.getRequest().getHeaders().getFirst("X-Internal-Token");
        if (internalToken.isBlank() || !internalToken.equals(token)) {
            log.warn("internal endpoint access denied, path={}",
                exchange.getRequest().getPath().value());
            return reject(exchange, HttpStatus.FORBIDDEN, "internal access denied", "14011");
        }
        return chain.filter(exchange);
    }

    /**
     * 认证后安全校验：写接口幂等键强制校验 + 敏感写接口防重放。
     *
     * @param exchange 已注入用户头的交换上下文
     * @param chain 过滤器链
     * @param path 请求路径
     * @param method HTTP方法
     * @return 过滤器执行结果
     */
    private Mono<Void> postAuthChecks(ServerWebExchange exchange, GatewayFilterChain chain,
                                      String path, String method) {
        if (!WRITE_METHODS.contains(method)) {
            return chain.filter(exchange);
        }

        String idempotencyKey = exchange.getRequest().getHeaders().getFirst("X-Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("missing Idempotency-Key, path={}, method={}", path, method);
            return reject(exchange, HttpStatus.BAD_REQUEST, "missing Idempotency-Key", "14010");
        }

        if (isSensitiveWrite(path)) {
            return checkAntiReplay(exchange, chain);
        }

        return chain.filter(exchange);
    }

    /**
     * 判断写路径是否为敏感接口，需要额外防重放校验。
     *
     * @param path 请求路径
     * @return true如果为敏感写接口
     */
    private boolean isSensitiveWrite(String path) {
        for (String prefix : SENSITIVE_WRITE_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 防重放校验：验证X-Client-Ts时间窗口和X-Client-Nonce一次性。
     *
     * @param exchange 服务端交换上下文
     * @param chain 过滤器链
     * @return 过滤器执行结果
     */
    private Mono<Void> checkAntiReplay(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ts = exchange.getRequest().getHeaders().getFirst("X-Client-Ts");
        String nonce = exchange.getRequest().getHeaders().getFirst("X-Client-Nonce");

        if (ts == null || ts.isBlank()) {
            return reject(exchange, HttpStatus.BAD_REQUEST, "missing X-Client-Ts", "14012");
        }
        if (nonce == null || nonce.length() < NONCE_MIN_LEN || nonce.length() > NONCE_MAX_LEN) {
            return reject(exchange, HttpStatus.BAD_REQUEST, "invalid X-Client-Nonce", "14012");
        }

        Long clientTs = parseTimestamp(ts);
        if (clientTs == null) {
            return reject(exchange, HttpStatus.BAD_REQUEST, "invalid X-Client-Ts", "14012");
        }

        long now = System.currentTimeMillis();
        if (Math.abs(now - clientTs) > TIMESTAMP_WINDOW_MS) {
            log.warn("request timestamp expired, clientTs={}, now={}", clientTs, now);
            return reject(exchange, HttpStatus.BAD_REQUEST, "request timestamp expired", "14012");
        }

        String nonceKey = "nonce:" + nonce;
        return redisTemplate.opsForValue()
            .setIfAbsent(nonceKey, "", NONCE_TTL)
            .flatMap(success -> {
                if (Boolean.TRUE.equals(success)) {
                    return chain.filter(exchange);
                }
                log.warn("replay detected, nonce={}", nonce);
                return reject(exchange, HttpStatus.BAD_REQUEST, "replay detected", "14012");
            });
    }

    /**
     * 安全解析时间戳字符串为epoch毫秒。
     *
     * @param ts 时间戳字符串
     * @return epoch毫秒值，解析失败返回null
     */
    private Long parseTimestamp(String ts) {
        try {
            return Long.parseLong(ts);
        }
        catch (NumberFormatException e) {
            return null;
        }
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
        String resolvedRole = role == null ? "USER" : role;
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        // 身份头以网关解析值为准：先清除客户端可能伪造的同名头再注入，防止水平越权
        builder.headers(h -> {
            h.remove("X-User-Id");
            h.remove("X-User-Role");
            h.remove("X-User-Jti");
            h.remove("X-Merchant-Id");
        });
        builder.header("X-User-Id", userId)
               .header("X-User-Role", resolvedRole)
               .header("X-User-Jti", jti == null ? "" : jti)
               .header("X-Trace-Id",
                   exchange.getRequest().getHeaders().getFirst("X-Trace-Id") == null
                       ? UUID.randomUUID().toString().replace("-", "")
                       : exchange.getRequest().getHeaders().getFirst("X-Trace-Id"));
        // 商家登录态：将账号ID作为merchantId透传，供goods/merchant等下游服务使用
        if (ROLE_MERCHANT.equals(resolvedRole)) {
            builder.header("X-Merchant-Id", userId);
        }
        return exchange.mutate().request(builder.build()).build();
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String reason, String code) {
        ServerHttpResponse res = exchange.getResponse();
        res.setStatusCode(status);
        res.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":\"" + code + "\",\"message\":\"" + reason + "\"}";
        return res.writeWith(Mono.just(res.bufferFactory().wrap(
            body.getBytes(StandardCharsets.UTF_8))));
    }

    /**
     * 获取过滤器执行顺序。
     *
     * @return 排序值
     */
    @Override
    public int getOrder() {
        return -50;
    }
}
