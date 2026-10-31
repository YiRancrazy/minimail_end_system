package com.yirancrazy.minimall.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JWT 签发与校验工具；使用配置密钥进行 HMAC 签名，并统一写入账号、角色和业务用户声明。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
public class JwtUtil {

  private final SecretKey key;

  /**
   * 使用配置中的 JWT 密钥初始化 HMAC 签名密钥。
   *
   * @param secret JWT 签名密钥，长度必须满足所选 HMAC 算法要求
   * @throws IllegalArgumentException 密钥长度不足以构造签名密钥时抛出
   */
  public JwtUtil(@Value("${minimall.jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 签发包含账号、角色、JTI、令牌类型和有效期的 JWT。
   * 当业务用户 ID 非空时额外写入 userId 声明。
   *
   * @param authUserId 认证账号主键，写入 subject
   * @param userId 业务用户 ID，可为空
   * @param account 登录账号
   * @param role 角色编码
   * @param roleId 持久化角色 ID
   * @param jti 令牌唯一标识
   * @param tokenType ACCESS 或 REFRESH
   * @param ttlMillis 有效时长，单位为毫秒
   * @return 已签名的紧凑 JWT 字符串
   */
  public String sign(
      Long authUserId,
      Long userId,
      String account,
      String role,
      Long roleId,
      String jti,
      String tokenType,
      long ttlMillis) {
    long now = System.currentTimeMillis();
    var builder =
        Jwts.builder()
            .subject(String.valueOf(authUserId))
            .claim("account", account)
            .claim("role", role)
            .claim("roleId", roleId)
            .claim("jti", jti)
            .claim("type", tokenType)
            .issuedAt(new Date(now))
            .expiration(new Date(now + ttlMillis))
            .signWith(key);
    if (userId != null) {
      builder.claim("userId", String.valueOf(userId));
    }
    return builder.compact();
  }

  /**
   * 生成去除连字符的随机 UUID，作为商家和平台账号的刷新令牌值。
   * 该方法只生成随机值，不负责写入 Redis。
   *
   * @return 32 位随机刷新令牌值
   */
  public String generateRefreshToken() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * 校验 JWT 签名和有效期后返回载荷。
   *
   * @param token 待解析的紧凑 JWT
   * @return 已验证的 JWT Claims
   * @throws io.jsonwebtoken.JwtException 令牌格式错误、签名不匹配或已过期时抛出
   */
  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
