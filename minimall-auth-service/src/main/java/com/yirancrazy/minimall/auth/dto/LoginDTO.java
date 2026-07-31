package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Login数据传输对象，用于Login相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data

@AllArgsConstructor

@NoArgsConstructor
public class LoginDTO {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}