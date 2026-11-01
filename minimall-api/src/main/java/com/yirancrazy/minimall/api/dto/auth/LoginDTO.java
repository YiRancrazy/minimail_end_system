package com.yirancrazy.minimall.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证服务内部登录请求，携带账号和密码.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class LoginDto {
    @NotBlank private String username;

    @NotBlank private String password;
}
