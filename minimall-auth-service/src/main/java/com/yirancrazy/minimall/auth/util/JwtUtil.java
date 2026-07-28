package com.yirancrazy.minimall.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtUtil(@Value("${minimall.jwt.secret}") String secret,
                   @Value("${minimall.jwt.ttl-seconds:86400}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlSeconds * 1000L;
    }

    public String sign(Long userId, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("role", role)
            .issuedAt(new Date(now))
            .expiration(new Date(now + ttlMillis))
            .signWith(key)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}