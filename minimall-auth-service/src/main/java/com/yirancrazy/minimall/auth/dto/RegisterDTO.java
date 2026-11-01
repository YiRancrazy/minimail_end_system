package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户和商家注册共用的账号密码入参；账号不可为空，密码长度限制为 6 至 64 个字符。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDto {
    @NotBlank private String account;

    @NotBlank
    @Size(min = 6, max = 64, message = "password length must be between 6 and 64")
    private String password;
}
