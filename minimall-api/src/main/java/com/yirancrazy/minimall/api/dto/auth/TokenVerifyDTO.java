package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JWT 令牌验签入参 DTO（具体承载字段见源码）。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
public class TokenVerifyDTO {
    private String token;
}