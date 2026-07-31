package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: TokenPayloadDTO description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class TokenPayloadDTO {
    private Long userId;
    private String role;
    private Long issuedAt;
    private Long expiresAt;
}