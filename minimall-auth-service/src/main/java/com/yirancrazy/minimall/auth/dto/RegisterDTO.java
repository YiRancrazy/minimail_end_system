package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
* 注册请求 DTO，承载 username 与 password。
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