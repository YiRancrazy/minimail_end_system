package com.yirancrazy.minimall.gateway.filter;

import com.yirancrazy.minimall.gateway.config.JwtVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthGlobalFilterTest {

    private static final String SECRET = "test-secret-1234567890123456789012";

    @Test
    void whitelist_path_passes_through() {
        JwtVerifier v = new JwtVerifier(SECRET);
        AuthGlobalFilter f = new AuthGlobalFilter(v);
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.post("/api/v1/auth/login"));
        Mono<Void> r = f.filter(ex, e -> Mono.empty());
        assertNotNull(r);
    }

    @Test
    void missing_authorization_returns_401() {
        JwtVerifier v = new JwtVerifier(SECRET);
        AuthGlobalFilter f = new AuthGlobalFilter(v);
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/user/1"));
        f.filter(ex, e -> Mono.empty()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getResponse().getStatusCode());
    }

    @Test
    void valid_token_passes_through_and_sets_headers() {
        JwtVerifier v = new JwtVerifier(SECRET);
        AuthGlobalFilter f = new AuthGlobalFilter(v);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
            .subject("42")
            .claim("username", "alice")
            .claim("role", "USER")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(key)
            .compact();
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/user/42")
                .header("Authorization", "Bearer " + token));
        f.filter(ex, e -> Mono.empty()).block();
        assertEquals("42", ex.getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("USER", ex.getRequest().getHeaders().getFirst("X-User-Role"));
    }
}