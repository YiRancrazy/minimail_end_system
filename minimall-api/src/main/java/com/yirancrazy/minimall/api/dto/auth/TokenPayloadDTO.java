package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;

/**
* JWT 令牌载荷 DTO，承载 userId、role、issuedAt 与 expiresAt 字段。
 */
@Data
public class TokenPayloadDTO {
    private Long userId;
    private String role;
    private Long issuedAt;
    private Long expiresAt;
}