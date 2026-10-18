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
 * @Description: JwtVerifier 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
public class JwtVerifier {

    private final SecretKey key;

    /**
     * 构造 JwtVerifier 实例。
     * @param secret secret 参数
     */
    public JwtVerifier(@Value("${minimall.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    /**
     * 执行 verify 方法。
     * @param token token 参数
     * @return 处理结果
     */
    public Claims verify(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}