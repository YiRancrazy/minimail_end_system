package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 解析 JWT 后得到的身份、角色和有效期载荷.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class TokenPayloadDto {
  private Long authUserId;
  private Long userId;
  private String role;
  private Long issuedAt;
  private Long expiresAt;
}
