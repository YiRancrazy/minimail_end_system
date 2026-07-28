package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;

@Data
public class TokenPayloadDTO {
    private Long userId;
    private String role;
    private Long issuedAt;
    private Long expiresAt;
}