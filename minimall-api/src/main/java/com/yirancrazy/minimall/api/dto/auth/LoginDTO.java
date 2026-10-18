package com.yirancrazy.minimall.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: LoginDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class LoginDTO {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}