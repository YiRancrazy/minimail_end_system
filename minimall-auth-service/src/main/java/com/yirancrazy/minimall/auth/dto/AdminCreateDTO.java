package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员创建 DTO，用于新增管理员入参校验
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class AdminCreateDTO {

    @NotBlank(message = "username cannot be blank")
    @Size(min = 4, max = 64, message = "username length must be between 4 and 64")
    private String username;

    @NotBlank(message = "password cannot be blank")
    @Size(min = 6, max = 128, message = "password length must be between 6 and 128")
    private String password;

    @Size(max = 64, message = "nickname length must be <= 64")
    private String nickname;
}
