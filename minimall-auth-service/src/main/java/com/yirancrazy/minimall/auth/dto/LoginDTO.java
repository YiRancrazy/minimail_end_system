package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
* 登录请求 DTO，承载 username 与 password（已通过 @Valid 校验）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}