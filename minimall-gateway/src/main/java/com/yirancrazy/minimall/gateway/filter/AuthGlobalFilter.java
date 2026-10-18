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
 * @Description: AuthGlobalFilter 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 提前编译JWT正则表达式，匹配标准的三段式JWT格式
    private static final Pattern JWT_PATTERN = Pattern.compile(
        "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    // 白名单
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

    // 匿名可读的前缀白名单：商品评价列表与统计供商品详情页未登录访问
    private static final Set<String> WHITELIST_PREFIXES = Set.of(
        "/api/v1/goods/comments"
    );

    private static final Map<String, Set<String>> ROLE_PATHS = Map.of(
        "/api/v1/user", Set.of("USER"),
        "/api/v1/merchant", Set.of("MERCHANT"),
        "/api/v1/platform", Set.of("PLATFORM"),
        "/api/v1/notify", Set.of("USER")
    );

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
     * 构造 AuthGlobalFilter 实例。
     * @param verifier verifier 参数
     * @param redisTemplate redisTemplate 参数
     * @param internalToken internalToken 参数
     */
    public AuthGlobalFilter(JwtVerifier verifier,
                            ReactiveStringRedisTemplate redisTemplate,
                            @Value("${minimall.security.internal-token:}") String internalToken) {
        this.verifier = verifier;
        this.redisTemplate = redisTemplate;
        this.internalToken = internalToken;
    }


    /**
     * 执行 filter 方法。
     * @param exchange exchange 参数
     * @param chain chain 参数
     * @return 处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();  // 获取请求路径
        String method = exchange.getRequest().getMethod().name();  // 获取请求方法

        // 判断是否在白名单中
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 判断是否是内部请求链接，是则进行内部请求验证
        if (path.startsWith("/internal/")) {
            return handleInternal(exchange, chain);
        }

        // 获取access_token，进行JWT验证
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "缺少 token", "14003");
        }
        String token = auth.substring(7);
        if (!JWT_PATTERN.matcher(token).matches()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "token 格式错误", "14003");
        }

        Claims claims;
        try {
            claims = verifier.verify(token);
        }
        catch (JwtException ex) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "token 无效", "14003");
        }

        String jti = claims.get("jti", String.class);
        String role = claims.get("role", String.class);
        if (role == null) {
            role = "USER";
        }

        if (!isAuthorized(path, role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, "访问被拒绝", "14007");
        }

        if (jti != null) {
            return redisTemplate.hasKey("blacklist:jti:" + jti)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return reject(exchange, HttpStatus.UNAUTHORIZED, "token 已失效", "14003");
                    }
                    // JTI模式的JWT验证通过，进行后续权限校验
                    ServerWebExchange mutated = mutateWithHeaders(exchange, claims, jti);
                    return postAuthChecks(mutated, chain, path, method);
                });
        }

        // 非JTI模式的JWT验证通过，进行后续权限校验(兼容旧代码)
        ServerWebExchange mutated = mutateWithHeaders(exchange, claims, jti);
        return postAuthChecks(mutated, chain, path, method);
    }


    /**
     * 处理Internal。
     * @param exchange exchange
     * @param chain chain
     * @return 处理结果
     */
    private Mono<Void> handleInternal(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求中携带的 X-Internal-Token
        String token = exchange.getRequest().getHeaders().getFirst("X-Internal-Token");
        if (internalToken.isBlank() || !internalToken.equals(token)) {
            log.warn("内部接口访问被拒绝，path={}",
                exchange.getRequest().getPath().value());
            return reject(exchange, HttpStatus.FORBIDDEN, "内部接口访问被拒绝", "14011");
        }
        return chain.filter(exchange);
    }


    /**
     * 处理post认证Checks。
     * @param exchange exchange
     * @param chain chain
     * @param path path
     * @param method method
     * @return 处理结果
     */
    private Mono<Void> postAuthChecks(ServerWebExchange exchange, GatewayFilterChain chain,
                                      String path, String method) {
        if (!WRITE_METHODS.contains(method)) {
            return chain.filter(exchange);
        }

        String idempotencyKey = exchange.getRequest().getHeaders().getFirst("X-Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("缺少 Idempotency-Key，path={}，method={}", path, method);
            return reject(exchange, HttpStatus.BAD_REQUEST, "缺少 Idempotency-Key", "14010");
        }

        if (isSensitiveWrite(path)) {
            return checkAntiReplay(exchange, chain);
        }

        return chain.filter(exchange);
    }


    /**
     * 校验SensitiveWrite。
     * @param path path
     * @return 是否满足条件
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
     * 校验AntiReplay。
     * @param exchange exchange
     * @param chain chain
     * @return 处理结果
     */
    private Mono<Void> checkAntiReplay(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ts = exchange.getRequest().getHeaders().getFirst("X-Client-Ts");
        String nonce = exchange.getRequest().getHeaders().getFirst("X-Client-Nonce");

        if (ts == null || ts.isBlank()) {
            return reject(exchange, HttpStatus.BAD_REQUEST, "缺少 X-Client-Ts", "14012");
        }
        if (nonce == null || nonce.length() < NONCE_MIN_LEN || nonce.length() > NONCE_MAX_LEN) {
            return reject(exchange, HttpStatus.BAD_REQUEST, "X-Client-Nonce 非法", "14012");
        }

        Long clientTs = parseTimestamp(ts);
        if (clientTs == null) {
            return reject(exchange, HttpStatus.BAD_REQUEST, "X-Client-Ts 非法", "14012");
        }

        long now = System.currentTimeMillis();
        if (Math.abs(now - clientTs) > TIMESTAMP_WINDOW_MS) {
            log.warn("请求时间戳已过期，clientTs={}，now={}", clientTs, now);
            return reject(exchange, HttpStatus.BAD_REQUEST, "请求时间戳已过期", "14012");
        }

        String nonceKey = "nonce:" + nonce;
        return redisTemplate.opsForValue()
            .setIfAbsent(nonceKey, "", NONCE_TTL)
            .flatMap(success -> {
                if (Boolean.TRUE.equals(success)) {
                    return chain.filter(exchange);
                }
                log.warn("检测到重放请求，nonce={}", nonce);
                return reject(exchange, HttpStatus.BAD_REQUEST, "检测到重放请求", "14012");
            });
    }


    /**
     * 转换或解析Timestamp。
     * @param ts ts
     * @return 处理结果
     */
    private Long parseTimestamp(String ts) {
        try {
            return Long.parseLong(ts);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 校验Authorized。
     * @param path path
     * @param role 角色
     * @return 是否满足条件
     */
    private boolean isAuthorized(String path, String role) {
        for (Map.Entry<String, Set<String>> entry : ROLE_PATHS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue().contains(role);
            }
        }
        return false;
    }


    /**
     * 校验Whitelisted。
     * @param path path
     * @return 是否满足条件
     */
    private boolean isWhitelisted(String path) {
        if (WHITELIST.contains(path)) {
            return true;
        }
        for (String prefix : WHITELIST_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }


    /**
     * 处理mutate携带Headers。
     * @param exchange exchange
     * @param claims claims
     * @param jti JWT ID
     * @return 处理结果
     */
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

    // 拒绝请求并返回JSON错误响应
    /**
     * 处理reject。
     * @param exchange exchange
     * @param status 状态
     * @param reason 原因
     * @param code 编码
     * @return 处理结果
     */
    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String reason, String code) {
        ServerHttpResponse res = exchange.getResponse();
        res.setStatusCode(status);
        res.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":\"" + code + "\",\"message\":\"" + reason + "\"}";
        return res.writeWith(Mono.just(res.bufferFactory().wrap(
            body.getBytes(StandardCharsets.UTF_8))));
    }


    /**
     * 获取相关数据。
     * @return 业务数值
     */
    @Override
    public int getOrder() {
        return -50;
    }
}
