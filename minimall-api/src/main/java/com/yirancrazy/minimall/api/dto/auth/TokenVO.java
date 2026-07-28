package com.yirancrazy.minimall.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenVO {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
}