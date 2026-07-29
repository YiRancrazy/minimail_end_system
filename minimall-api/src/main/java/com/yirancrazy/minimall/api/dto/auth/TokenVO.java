package com.yirancrazy.minimall.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JWT 令牌返回视图，承载 accessToken、tokenType 与 expiresIn。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@AllArgsConstructor
public class TokenVO {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
}