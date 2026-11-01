package com.yirancrazy.minimall.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 登录或刷新成功后返回的访问令牌、刷新令牌和有效期.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenVo {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}
