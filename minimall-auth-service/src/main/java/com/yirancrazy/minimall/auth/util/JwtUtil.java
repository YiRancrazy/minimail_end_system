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
 * @Description: JwtUtil 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long ttlMillis;

    /**
     * 构造 JwtUtil 实例。
     * @param secret secret 参数
     * @param ttlSeconds ttlSeconds 参数
     */
    public JwtUtil(@Value("${minimall.jwt.secret}") String secret,
                   @Value("${minimall.jwt.ttl-seconds:900}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlSeconds * 1000L;
    }


    /**
     * 执行 sign 方法。
     * @param userId userId 参数
     * @param account account 参数
     * @param role role 参数
     * @param roleId roleId 参数
     * @param jti jti 参数
     * @return 处理结果
     */
    public String sign(Long userId, String account, String role, Long roleId, String jti) {
        return sign(userId, account, role, roleId, jti, ttlMillis);
    }


    /**
     * 执行 sign 方法。
     * @param userId userId 参数
     * @param account account 参数
     * @param role role 参数
     * @param roleId roleId 参数
     * @param jti jti 参数
     * @param ttlMillis ttlMillis 参数
     * @return 处理结果
     */
    public String sign(Long userId, String account, String role, Long roleId, String jti, long ttlMillis) {
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
     * 执行 generateRefreshToken 方法。
     * @return 处理结果
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }


    /**
     * 执行 parse 方法。
     * @param token token 参数
     * @return 处理结果
     */
    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
