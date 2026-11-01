package com.yirancrazy.minimall.gateway.filter;

import com.yirancrazy.minimall.gateway.config.JwtVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
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
import reactor.core.publisher.Mono;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在请求进入业务服务前执行网关边界安全控制。负责白名单和内部令牌规则、校验 JWT
 *   类型/签名/黑名单状态、检查角色路径权限、清除伪造身份头，并通过幂等和防重放检查保护写请求。
 *   业务校验仍由下游服务负责。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 提前编译JWT正则表达式，匹配标准的三段式JWT格式
    private static final Pattern JWT_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    // 白名单
    private static final Set<String> WHITELIST =
            Set.of(
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
                    "/actuator/health");

    // 匿名可读的前缀白名单：商品评价列表与统计供商品详情页未登录访问
    private static final Set<String> WHITELIST_PREFIXES = Set.of("/api/v1/goods/comments");

    private static final Map<String, Set<String>> ROLE_PATHS =
            Map.of(
                    "/api/v1/user", Set.of("USER"),
                    "/api/v1/merchant", Set.of("MERCHANT"),
                    "/api/v1/platform", Set.of("PLATFORM"),
                    "/api/v1/notify", Set.of("USER"));

    private static final String ROLE_MERCHANT = "MERCHANT";

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final long TIMESTAMP_WINDOW_MS = 5 * 60 * 1000L;

    private static final Duration NONCE_TTL = Duration.ofMinutes(10);

    private static final int NONCE_MIN_LEN = 6;

    private static final int NONCE_MAX_LEN = 16;

    private static final Set<String> SENSITIVE_WRITE_PREFIXES =
            Set.of("/api/v1/user/pay/", "/api/v1/merchant/pay/", "/api/v1/platform/pay/");

    private final JwtVerifier verifier;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final String internalToken;

    /**
     * 创建网关安全过滤器，并注入签名校验与防重放依赖。
     *
     * @param verifier JWT 签名校验器
     * @param redisTemplate 用于 JTI 黑名单和随机数校验的响应式 Redis 客户端
     * @param internalToken 内部路由共享令牌；为空时禁用内部路由访问
     */
    public AuthGlobalFilter(
            JwtVerifier verifier,
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${minimall.security.internal-token:}") String internalToken) {
        this.verifier = verifier;
        this.redisTemplate = redisTemplate;
        this.internalToken = internalToken;
    }

    /**
     * 依次执行白名单、内部令牌、JWT、权限和写请求安全检查。
     *
     * <p>被拒绝的请求会返回指定 HTTP 状态和 JSON 响应体。Redis 查询失败会作为响应式错误继续传播。
     * 通过校验的请求使用仅由已验签声明派生的身份头继续访问下游。
     *
     * @param exchange 当前网关请求和响应交换对象
     * @param chain 剩余网关过滤器链
     * @return 下游过滤器链或拒绝响应的完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value(); // 获取请求路径
        final String method = exchange.getRequest().getMethod().name(); // 获取请求方法

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
        } catch (JwtException ex) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "token 无效", "14003");
        }

        final String jti = claims.get("jti", String.class);
        String role = claims.get("role", String.class);
        String type = claims.get("type", String.class);
        if (role == null) {
            role = "USER";
        }

        if (!"access".equals(type)) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "token 类型错误", "14003");
        }

        if (!isAuthorized(path, role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, "访问被拒绝", "14007");
        }

        if (jti != null) {
            return redisTemplate
                    .hasKey("blacklist:jti:" + jti)
                    .flatMap(
                            blacklisted -> {
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
     * 校验 /internal/ 路径要求的共享令牌。
     *
     * <p>未配置令牌或请求携带的 X-Internal-Token 不匹配时失败关闭。该检查不授予业务访问权限，
     * 也不校验 JWT。
     *
     * @param exchange 当前请求和响应交换对象
     * @param chain 剩余网关过滤器链
     * @return 校验通过时返回下游完成信号，否则返回 403 拒绝响应
     */
    private Mono<Void> handleInternal(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求中携带的 X-Internal-Token
        String token = exchange.getRequest().getHeaders().getFirst("X-Internal-Token");
        if (internalToken.isBlank() || !internalToken.equals(token)) {
            log.warn("内部接口访问被拒绝，path={}", exchange.getRequest().getPath().value());
            return reject(exchange, HttpStatus.FORBIDDEN, "内部接口访问被拒绝", "14011");
        }
        return chain.filter(exchange);
    }

    /**
     * 在鉴权和权限校验通过后执行写请求保护。
     *
     * <p>POST、PUT、PATCH 和 DELETE 请求必须携带非空 X-Idempotency-Key。敏感支付路径还必须携带
     * 有效期内的客户端时间戳和一次性随机数。
     *
     * @param exchange 已携带可信身份头并通过鉴权的请求交换对象
     * @param chain 剩余网关过滤器链
     * @param path 已用于权限判断的请求路径
     * @param method HTTP 方法名
     * @return 校验通过时返回下游完成信号，否则返回 400 拒绝响应
     */
    private Mono<Void> postAuthChecks(
            ServerWebExchange exchange, GatewayFilterChain chain, String path, String method) {
        if (!WRITE_METHODS.contains(method)) {
            return chain.filter(exchange);
        }

        // 获取请求中携带的 X-Idempotency-Key
        String idempotencyKey = exchange.getRequest().getHeaders().getFirst("X-Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("缺少 Idempotency-Key，path={}，method={}", path, method);
            return reject(exchange, HttpStatus.BAD_REQUEST, "缺少 Idempotency-Key", "14010");
        }

        // 对于敏感写操作，进行抗重放攻击校验
        if (isSensitiveWrite(path)) {
            return checkAntiReplay(exchange, chain);
        }

        return chain.filter(exchange);
    }

    /**
     * 判断路径在通过幂等检查后是否还需要防重放保护。
     *
     * @param path 请求路径
     * @return 路径以已配置的支付前缀开头时返回 true
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
     * 校验请求时效性，并在 Redis 中原子记录客户端随机数。
     *
     * <p>时间戳必须为毫秒级 Unix 时间，且与网关时间相差不超过五分钟。随机数长度必须为 6 到 16
     * 字符，仅在不存在时写入并保留十分钟。Redis 存储失败会作为响应式错误继续传播。
     *
     * @param exchange 已鉴权的敏感写请求交换对象
     * @param chain 剩余网关过滤器链
     * @return 新请求返回下游完成信号，否则返回 400 拒绝响应
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
        return redisTemplate
                .opsForValue()
                .setIfAbsent(nonceKey, "", NONCE_TTL)
                .flatMap(
                        success -> {
                            if (Boolean.TRUE.equals(success)) {
                                return chain.filter(exchange);
                            }
                            log.warn("检测到重放请求，nonce={}", nonce);
                            return reject(exchange, HttpStatus.BAD_REQUEST, "检测到重放请求", "14012");
                        });
    }

    /**
     * 解析毫秒级 Unix 时间戳，并避免格式错误绕过过滤器。
     *
     * @param ts 十进制毫秒级时间戳文本
     * @return 解析后的时间戳；值不是有效 long 时返回 null
     */
    private Long parseTimestamp(String ts) {
        try {
            return Long.parseLong(ts);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 检查已验签 JWT 中的角色是否允许访问请求路径。
     *
     * <p>角色匹配使用第一个命中的已配置路径前缀。未命中任何前缀的路径默认拒绝。
     *
     * @param path 请求路径
     * @param role 从已验签 JWT 声明中解析出的角色
     * @return 仅当命中的路径前缀允许该角色时返回 true
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
     * 检查精确公开端点及其显式允许的路径族。
     *
     * <p>前缀匹配要求路径等于前缀或在前缀后紧接斜杠，防止相似路径绕过鉴权。
     *
     * @param path 请求路径
     * @return 请求可以跳过 JWT 鉴权时返回 true
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
     * 使用已验签 JWT 声明派生值替换客户端可控的身份头。
     *
     * <p>X-User-Id 缺失时回退到 JWT subject，角色缺失时默认为 USER。链路标识缺失时会生成新值，
     * 商家账号会注入 X-Merchant-Id。本方法不执行下游业务授权。
     *
     * @param exchange 已鉴权的请求交换对象
     * @param claims 签名校验成功后返回的声明
     * @param jti 令牌标识；兼容旧令牌时可为 null
     * @return 下游请求包含可信网关身份头的交换对象
     */
    private ServerWebExchange mutateWithHeaders(
            ServerWebExchange exchange, Claims claims, String jti) {
        String authUserId = claims.getSubject();
        String userId = claims.get("userId", String.class);
        if (userId == null || userId.isBlank()) {
            userId = authUserId;
        }
        String role = claims.get("role", String.class);
        String resolvedRole = role == null ? "USER" : role;
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        // 身份头以网关解析值为准：先清除客户端可能伪造的同名头再注入，防止水平越权
        builder.headers(
                h -> {
                    h.remove("X-Auth-User-Id");
                    h.remove("X-User-Id");
                    h.remove("X-User-Role");
                    h.remove("X-User-Jti");
                    h.remove("X-Merchant-Id");
                });
        builder
                .header("X-Auth-User-Id", authUserId)
                .header("X-User-Id", userId)
                .header("X-User-Role", resolvedRole)
                .header("X-User-Jti", jti == null ? "" : jti)
                .header(
                        "X-Trace-Id",
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
     * 写入网关拒绝状态和 JSON 错误响应体并完成响应。
     *
     * @param exchange 需要写入响应的交换对象
     * @param status HTTP 拒绝状态
     * @param reason 客户端可见的错误消息
     * @param code 应用错误码
     * @return 响应体写入完成信号
     */
    private Mono<Void> reject(
            ServerWebExchange exchange, HttpStatus status, String reason, String code) {
        ServerHttpResponse res = exchange.getResponse();
        res.setStatusCode(status);
        res.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":\"" + code + "\",\"message\":\"" + reason + "\"}";
        return res.writeWith(
                Mono.just(res.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
    }

    /**
     * 将鉴权安排在链路标识透传之后、下游业务处理之前。
     *
     * @return 过滤器顺序 -50
     */
    @Override
    public int getOrder() {
        return -50;
    }
}
