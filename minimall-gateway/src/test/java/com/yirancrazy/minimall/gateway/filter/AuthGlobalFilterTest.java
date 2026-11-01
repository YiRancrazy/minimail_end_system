package com.yirancrazy.minimall.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.gateway.config.JwtVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用真实 HMAC JWT 校验器和模拟 Redis 操作验证网关鉴权与请求加固行为。场景覆盖
 *   白名单、角色边界、黑名单与令牌类型、可信身份头注入、内部路由、幂等和防重放校验，测试期间不
 *   启动网关。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class AuthGlobalFilterTest {

    private static final String SECRET = "test-secret-1234567890123456789012";

    private static final String INTERNAL_TOKEN = "test-internal-token";

    private static final String ACCESS_TOKEN_TYPE = "access";

    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Mock private ReactiveStringRedisTemplate redisTemplate;

    @Mock private ReactiveValueOperations<String, String> valueOps;

    private JwtVerifier verifier;
    private AuthGlobalFilter filter;

    /**
     * 使用测试密钥和模拟 Redis 客户端创建过滤器。
     */
    @BeforeEach
    void setUp() {
        verifier = new JwtVerifier(SECRET);
        filter = new AuthGlobalFilter(verifier, redisTemplate, INTERNAL_TOKEN);
    }

    /** 验证登录白名单免认证；前置为 POST 登录路径且无令牌；执行过滤器；预期返回非空流水线且不写拒绝响应. */
    @Test
    void testWhitelistPathPassesThrough() {
        ServerWebExchange ex =
                MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/user/auth/login"));
        Mono<Void> r = filter.filter(ex, e -> Mono.empty());
        assertNotNull(r);
    }

    /** 验证受保护路径缺失令牌被拒；前置为 GET 用户路径且无认证头；执行过滤器；预期返回 401. */
    @Test
    void testMissingAuthorizationReturns401() {
        ServerWebExchange ex =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/user/users/1"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    /** 验证商品评论允许匿名读取；前置为 GET 评论路径且无令牌；执行过滤器；预期不写拒绝状态. */
    @Test
    void testAnonymousGoodsCommentsAreWhitelisted() {
        ServerWebExchange ex =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/goods/comments/1001"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertNull(ex.getResponse().getStatusCode());
    }

    /** 验证用户令牌可访问通知端点；前置为有效用户 JWT 且 Redis 无黑名单；执行过滤；预期透传用户标识. */
    @Test
    void testUserTokenCanReachNotifyEndpoint() {
        String token = buildToken("42", "USER", "jti-sse");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/notify/sse")
                                .header("Authorization", "Bearer " + token));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals("42", ex.getRequest().getHeaders().getFirst("X-User-Id"));
    }

    /** 验证商家令牌不可访问通知端点；前置为有效商家 JWT；执行过滤；预期返回 403. */
    @Test
    void testMerchantTokenCannotReachNotifyEndpoint() {
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/notify/sse")
                                .header("Authorization", "Bearer " + token("200", "MERCHANT")));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, ex.getResponse().getStatusCode());
    }

    /** 验证有效令牌注入身份头；前置为声明含 subject、userId、role；执行过滤；预期三类可信头使用声明值. */
    @Test
    void testValidTokenPassesThroughAndSetsHeaders() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token =
                Jwts.builder()
                        .subject("42")
                        .claim("username", "alice")
                        .claim("role", "USER")
                        .claim("userId", "9001")
                        .claim("jti", "test-jti")
                        .claim("type", ACCESS_TOKEN_TYPE)
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + 3600_000))
                        .signWith(key)
                        .compact();

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/user/users/42")
                                .header("Authorization", "Bearer " + token));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals("42", ex.getRequest().getHeaders().getFirst("X-Auth-User-Id"));
        assertEquals("9001", ex.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("USER", ex.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    /** 验证商家令牌注入商家标识；前置为商家主题 200 且有效 JWT；执行过滤；预期用户与商家头均为 200. */
    @Test
    void testMerchantTokenInjectsMerchantIdHeader() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token =
                Jwts.builder()
                        .subject("200")
                        .claim("username", "shopowner")
                        .claim("role", "MERCHANT")
                        .claim("jti", "merch-jti")
                        .claim("type", ACCESS_TOKEN_TYPE)
                        .issuedAt(new Date())
                        .expiration(new Date(System.currentTimeMillis() + 3600_000))
                        .signWith(key)
                        .compact();

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/merchant/goods")
                                .header("Authorization", "Bearer " + token));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals("200", ex.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("200", ex.getRequest().getHeaders().getFirst("X-Auth-User-Id"));
        assertEquals("MERCHANT", ex.getRequest().getHeaders().getFirst("X-User-Role"));
        assertEquals("200", ex.getRequest().getHeaders().getFirst("X-Merchant-Id"));
    }

    /** 验证商家令牌不可跨入用户路径；前置为有效商家 JWT；执行过滤；预期返回 403. */
    @Test
    void testMerchantTokenCannotAccessUserPath() {
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/user/users/42")
                                .header("Authorization", "Bearer " + token("42", "MERCHANT")));

        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, ex.getResponse().getStatusCode());
    }

    /** 验证平台令牌不可跨入商家路径；前置为有效平台 JWT；执行过滤；预期返回 403. */
    @Test
    void testPlatformTokenCannotAccessMerchantPath() {
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/merchant/orders")
                                .header("Authorization", "Bearer " + token("9", "PLATFORM")));

        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, ex.getResponse().getStatusCode());
    }

    /** 验证刷新令牌不可访问受保护端点；前置为刷新类型 JWT；执行过滤；预期返回 401. */
    @Test
    void testRefreshTokenCannotAccessProtectedEndpoint() {
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/user/users/42")
                                .header(
                                        "Authorization",
                                        "Bearer " + buildToken("42", "USER", "refresh-jti", REFRESH_TOKEN_TYPE)));

        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    /** 验证伪造身份头被覆盖；前置为有效用户 JWT 与伪造用户、角色头；执行过滤；预期改为声明值. */
    @Test
    void testForgedIdentityHeadersAreClearedAndReplaced() {
        String token = buildToken("123", "USER", "jti-forged");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        // 客户端预置伪造身份头，网关必须清除后注入令牌解析值，防止水平越权
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/v1/user/users/123")
                                .header("Authorization", "Bearer " + token)
                                .header("X-User-Id", "999999")
                                .header("X-User-Role", "PLATFORM"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals("123", ex.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("USER", ex.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    /** 验证内部接口缺失共享令牌时失败关闭；前置为已配置内部令牌；执行过滤；预期返回 403. */
    @Test
    void testInternalEndpointWithoutTokenReturns403() {
        ServerWebExchange ex =
                MockServerWebExchange.from(MockServerHttpRequest.get("/internal/order/123"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, ex.getResponse().getStatusCode());
    }

    /** 验证内部接口携带正确共享令牌可放行；前置为请求头与配置一致；执行过滤；预期无拒绝并完成. */
    @Test
    void testInternalEndpointWithCorrectTokenPasses() {
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/internal/order/123")
                                .header("X-Internal-Token", INTERNAL_TOKEN));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
    }

    /** 验证受保护写请求必须携带幂等键；前置为有效令牌且 POST 缺失幂等头；执行过滤；预期返回 400. */
    @Test
    void testWriteWithoutIdempotencyKeyReturns400() {
        String token = buildToken("42", "USER", "jti-write-1");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.post("/api/v1/user/users/42")
                                .header("Authorization", "Bearer " + token));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatusCode());
    }

    /** 验证普通写请求携带幂等键可放行；前置为有效令牌与非空幂等头；执行过滤；预期完成且不因幂等拒绝. */
    @Test
    void testWriteWithIdempotencyKeyPasses() {
        String token = buildToken("42", "USER", "jti-write-2");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.post("/api/v1/user/users/42")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Idempotency-Key", "idem-uuid-123"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
    }

    /** 验证敏感写请求必须携带防重放头；前置为认证与幂等通过但缺失时间戳；执行过滤；预期返回 400. */
    @Test
    void testSensitiveWriteWithoutAntiReplayHeadersReturns400() {
        String token = buildToken("42", "USER", "jti-pay-1");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.post("/api/v1/user/pay/create")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Idempotency-Key", "idem-uuid-456"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatusCode());
    }

    /** 验证新鲜敏感写请求可通过防重放校验；前置为时间戳合法且 Redis 占用随机数成功；执行过滤；预期完成. */
    @Test
    void testSensitiveWriteWithValidAntiReplayHeadersPasses() {
        String token = buildToken("42", "USER", "jti-pay-2");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(true));

        long ts = System.currentTimeMillis();
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.post("/api/v1/user/pay/create")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Idempotency-Key", "idem-uuid-789")
                                .header("X-Client-Ts", String.valueOf(ts))
                                .header("X-Client-Nonce", "nonce123abc"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
    }

    /** 验证重复随机数被判为重放；前置为 Redis 写入返回已存在；执行过滤；预期返回 400. */
    @Test
    void testSensitiveWriteWithReplayedNonceReturns400() {
        String token = buildToken("42", "USER", "jti-pay-3");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Mono.just(false));

        long ts = System.currentTimeMillis();
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.post("/api/v1/user/pay/create")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Idempotency-Key", "idem-uuid-replay")
                                .header("X-Client-Ts", String.valueOf(ts))
                                .header("X-Client-Nonce", "replaynonce1"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatusCode());
    }

    /** 验证过期时间戳被拒；前置为敏感写请求时间戳早十分钟；执行过滤；预期返回 400 且不写随机数. */
    @Test
    void testSensitiveWriteWithExpiredTimestampReturns400() {
        String token = buildToken("42", "USER", "jti-pay-4");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        long expiredTs = System.currentTimeMillis() - 10 * 60 * 1000L;
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.post("/api/v1/user/pay/create")
                                .header("Authorization", "Bearer " + token)
                                .header("X-Idempotency-Key", "idem-uuid-expired")
                                .header("X-Client-Ts", String.valueOf(expiredTs))
                                .header("X-Client-Nonce", "nonce456def"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty())).verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatusCode());
    }

    /**
     * 使用指定身份和令牌标识构建签名测试访问令牌。
     *
     * @param subject JWT 主题
     * @param role 角色声明
     * @param jti JWT 标识
     * @return 紧凑签名访问令牌
     */
    private String buildToken(String subject, String role, String jti) {
        return buildToken(subject, role, jti, ACCESS_TOKEN_TYPE);
    }

    /**
     * 使用显式令牌类型构建签名测试令牌，用于鉴权失败场景。
     *
     * @param subject JWT 主题
     * @param role 角色声明
     * @param jti JWT 标识
     * @param tokenType 访问令牌或刷新令牌类型
     * @return 紧凑签名 JWT
     */
    private String buildToken(String subject, String role, String jti, String tokenType) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("jti", jti)
                .claim("type", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

    /**
     * 使用角色派生 JTI 构建签名访问令牌，供权限校验测试使用。
     *
     * @param subject JWT 主题
     * @param role 角色声明
     * @return 紧凑签名访问令牌
     */
    private String token(String subject, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("jti", "test-" + role)
                .claim("type", ACCESS_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }
}
