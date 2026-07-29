package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JWT 令牌载荷 DTO，承载 userId、role、issuedAt 与 expiresAt 字段。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
public class TokenPayloadDTO {
    private Long userId;
    private String role;
    private Long issuedAt;
    private Long expiresAt;
}