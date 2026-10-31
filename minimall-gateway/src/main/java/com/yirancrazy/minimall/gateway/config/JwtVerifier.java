package com.yirancrazy.minimall.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在网关边界校验 JWT 签名并提取声明。HMAC 密钥来自 minimall.jwt.secret，令牌类型、
 *   角色授权和黑名单判断仍由 AuthGlobalFilter 负责。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
public class JwtVerifier {

  private final SecretKey key;

  /**
   * 创建 JWT 校验器，并从 UTF-8 编码的配置密钥派生 HMAC 密钥。
   *
   * <p>当密钥长度不足以满足 JJWT 所选 HMAC 算法时，Bean 创建失败。
   *
   * @param secret 应用配置提供的共享 JWT 签名密钥
   */
  public JwtVerifier(@Value("${minimall.jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 解析签名 JWT，校验签名和注册的时间声明，并返回载荷。
   *
   * <p>本方法不校验令牌类型、角色路径授权或 JTI 黑名单。
   *
   * @param token 不包含 Bearer 前缀的紧凑签名 JWT
   * @return 校验通过的 JWT 声明
   * @throws io.jsonwebtoken.JwtException 当令牌格式错误、已过期、不受支持或签名无效时抛出
   */
  public Claims verify(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
