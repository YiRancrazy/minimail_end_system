package com.yirancrazy.minimall.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 视图对象，用于返回响应数据。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenVO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}