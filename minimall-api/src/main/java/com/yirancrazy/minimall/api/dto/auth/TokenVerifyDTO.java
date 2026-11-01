package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 待认证服务校验和解析的 JWT 令牌.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class TokenVerifyDto {
    private String token;
}
