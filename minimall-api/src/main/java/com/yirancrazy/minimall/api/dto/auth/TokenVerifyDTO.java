package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;

/**
* JWT 令牌验签入参 DTO（具体承载字段见源码）。
 */
@Data
public class TokenVerifyDTO {
    private String token;
}