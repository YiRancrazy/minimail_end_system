package com.yirancrazy.minimall.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 数据传输对象，用于接收请求参数。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Data
public class LoginDTO {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}