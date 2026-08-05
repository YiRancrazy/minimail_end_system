package com.yirancrazy.minimall.gateway.filter;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import com.yirancrazy.minimall.gateway.config.JwtVerifier;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthGlobalFilter 的单元测试类，覆盖JWT鉴权、内部接口隔离、幂等键校验和防重放。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@ExtendWith(MockitoExtension.class)
class AuthGlobalFilterTest {

    private static final String SECRET = "test-secret-1234567890123456789012";

    private static final String INTERNAL_TOKEN = "test-internal-token";

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    private JwtVerifier verifier;
    private AuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        verifier = new JwtVerifier(SECRET);
        filter = new AuthGlobalFilter(verifier, redisTemplate, INTERNAL_TOKEN);
    }

    @Test
    void whitelist_path_passes_through() {
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/user/auth/login"));
        Mono<Void> r = filter.filter(ex, e -> Mono.empty());
        assertNotNull(r);
    }

    @Test
    void missing_authorization_returns_401() {
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/user/users/1"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void valid_token_passes_through_and_sets_headers() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
            .subject("42")
            .claim("username", "alice")
            .claim("role", "USER")
            .claim("jti", "test-jti")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(key)
            .compact();

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/user/users/42")
                .header("Authorization", "Bearer " + token));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
        assertEquals("42", ex.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("USER", ex.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void merchant_token_injects_merchant_id_header() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
            .subject("200")
            .claim("username", "shopowner")
            .claim("role", "MERCHANT")
            .claim("jti", "merch-jti")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(key)
            .compact();

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/merchant/goods")
                .header("Authorization", "Bearer " + token));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
        assertEquals("200", ex.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("MERCHANT", ex.getRequest().getHeaders().getFirst("X-User-Role"));
        assertEquals("200", ex.getRequest().getHeaders().getFirst("X-Merchant-Id"));
    }

    @Test
    void internal_endpoint_without_token_returns_403() {
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/internal/order/123"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
        assertEquals(HttpStatus.FORBIDDEN, ex.getResponse().getStatusCode());
    }

    @Test
    void internal_endpoint_with_correct_token_passes() {
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/internal/order/123")
                .header("X-Internal-Token", INTERNAL_TOKEN));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
    }

    @Test
    void write_without_idempotency_key_returns_400() {
        String token = buildToken("42", "USER", "jti-write-1");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/user/users/42")
                .header("Authorization", "Bearer " + token));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatusCode());
    }

    @Test
    void write_with_idempotency_key_passes() {
        String token = buildToken("42", "USER", "jti-write-2");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/user/users/42")
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", "idem-uuid-123"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
    }

    @Test
    void sensitive_write_without_anti_replay_headers_returns_400() {
        String token = buildToken("42", "USER", "jti-pay-1");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/user/pay/create")
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", "idem-uuid-456"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatusCode());
    }

    @Test
    void sensitive_write_with_valid_anti_replay_headers_passes() {
        String token = buildToken("42", "USER", "jti-pay-2");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Mono.just(true));

        long ts = System.currentTimeMillis();
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/user/pay/create")
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", "idem-uuid-789")
                .header("X-Client-Ts", String.valueOf(ts))
                .header("X-Client-Nonce", "nonce123abc"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
    }

    @Test
    void sensitive_write_with_replayed_nonce_returns_400() {
        String token = buildToken("42", "USER", "jti-pay-3");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(Mono.just(false));

        long ts = System.currentTimeMillis();
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/user/pay/create")
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", "idem-uuid-replay")
                .header("X-Client-Ts", String.valueOf(ts))
                .header("X-Client-Nonce", "replaynonce1"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatusCode());
    }

    @Test
    void sensitive_write_with_expired_timestamp_returns_400() {
        String token = buildToken("42", "USER", "jti-pay-4");
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        long expiredTs = System.currentTimeMillis() - 10 * 60 * 1000L;
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/user/pay/create")
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", "idem-uuid-expired")
                .header("X-Client-Ts", String.valueOf(expiredTs))
                .header("X-Client-Nonce", "nonce456def"));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatusCode());
    }

    private String buildToken(String subject, String role, String jti) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .subject(subject)
            .claim("role", role)
            .claim("jti", jti)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(key)
            .compact();
    }
}
