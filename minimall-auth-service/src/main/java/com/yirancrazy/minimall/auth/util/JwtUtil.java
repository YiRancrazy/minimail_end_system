package com.yirancrazy.minimall.auth.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JWT工具类，提供令牌签发与解析能力。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtUtil(@Value("${minimall.jwt.secret}") String secret,
                   @Value("${minimall.jwt.ttl-seconds:900}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlSeconds * 1000L;
    }

    /**
     * 生成JWT令牌。
     * @param userId 用户ID
     * @param account 登录账号
     * @param role 角色编码
     * @param roleId 角色ID
     * @param jti 令牌唯一标识
     * @return JWT令牌
     */
    public String sign(Long userId, String account, String role, Long roleId, String jti) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("account", account)
            .claim("role", role)
            .claim("roleId", roleId)
            .claim("jti", jti)
            .issuedAt(new Date(now))
            .expiration(new Date(now + ttlMillis))
            .signWith(key)
            .compact();
    }

    /**
     * 生成刷新令牌。
     * @return 刷新令牌
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 解析JWT令牌。
     * @param token JWT令牌
     * @return JWT声明
     */
    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
