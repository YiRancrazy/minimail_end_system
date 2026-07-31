package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Register数据传输对象，用于Register相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class RegisterDTO {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}