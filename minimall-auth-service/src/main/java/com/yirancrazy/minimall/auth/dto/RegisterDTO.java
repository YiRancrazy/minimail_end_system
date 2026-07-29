package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 注册请求 DTO，承载 username 与 password。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}