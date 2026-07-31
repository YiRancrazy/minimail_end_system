package com.yirancrazy.minimall.gateway.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JwtVerifier，提供网关相关能力
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class JwtVerifier {

    private final SecretKey key;

    public JwtVerifier(@Value("${minimall.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验签 JWT 并返回 Claims 载荷，供网关过滤器读取用户身份信息。
     *
     * @param token 客户端 Authorization 头中的 JWT 字符串
     * @return 解析后的 Claims 载荷，包含 subject 与自定义声明
     */
    public Claims verify(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}