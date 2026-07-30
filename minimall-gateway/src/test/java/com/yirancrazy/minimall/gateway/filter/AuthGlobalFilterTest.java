package com.yirancrazy.minimall.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import com.yirancrazy.minimall.gateway.config.JwtVerifier;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthGlobalFilter 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@ExtendWith(MockitoExtension.class)
class AuthGlobalFilterTest {

    private static final String SECRET = "test-secret-1234567890123456789012";

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private JwtVerifier verifier;
    private AuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        verifier = new JwtVerifier(SECRET);
        filter = new AuthGlobalFilter(verifier, redisTemplate);
    }

    @Test
    void whitelist_path_passes_through() {
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/auth/login"));
        Mono<Void> r = filter.filter(ex, e -> Mono.empty());
        assertNotNull(r);
    }

    @Test
    void missing_authorization_returns_401() {
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/user/1"));
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
            .issuedAt(Instant.now())
            .expiration(Instant.now().plusSeconds(3600))
            .signWith(key)
            .compact();

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/user/42")
                .header("Authorization", "Bearer " + token));
        StepVerifier.create(filter.filter(ex, e -> Mono.empty()))
            .verifyComplete();
        assertEquals("42", ex.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("USER", ex.getRequest().getHeaders().getFirst("X-User-Role"));
    }
}