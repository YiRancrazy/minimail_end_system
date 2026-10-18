package com.yirancrazy.minimall.api.dto.auth;

import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: TokenPayloadDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class TokenPayloadDTO {
    private Long userId;
    private String role;
    private Long issuedAt;
    private Long expiresAt;
}