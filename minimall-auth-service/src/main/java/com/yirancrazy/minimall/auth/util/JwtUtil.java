package com.yirancrazy.minimall.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JWT 工具类，负责访问令牌签发与 Claims 解析。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtUtil(@Value("${minimall.jwt.secret}") String secret,
                   @Value("${minimall.jwt.ttl-seconds:86400}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlSeconds * 1000L;
    }

    /**
     * 使用 HMAC-SHA 对用户ID、用户名与角色进行签名，生成含过期时间的 JWT 访问令牌。
     *
     * @param userId 用户唯一标识，将作为 subject 写入令牌
     * @param username 用户名，写入 username 声明
     * @param role 角色标识，写入 role 声明
     * @return 已签名的 JWT 字符串
     */
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

    /**
     * 校验并解析 JWT 令牌，返回其 Claims 载荷（含 subject / username / role 等）。
     *
     * @param token 待解析的 JWT 字符串
     * @return 令牌中的 Claims 载荷对象
     */
    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}