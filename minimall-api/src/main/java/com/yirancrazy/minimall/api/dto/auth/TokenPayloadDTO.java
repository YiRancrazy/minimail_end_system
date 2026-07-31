package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: TokenPayload数据传输对象，用于TokenPayload相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
public class TokenPayloadDTO {
    private Long userId;
    private String role;
    private Long issuedAt;
    private Long expiresAt;
}