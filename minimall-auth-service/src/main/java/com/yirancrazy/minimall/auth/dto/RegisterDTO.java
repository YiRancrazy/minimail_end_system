package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data

@AllArgsConstructor

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RegisterDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@NoArgsConstructor
public class RegisterDTO {
    @NotBlank
    private String account;

    @NotBlank
    @Size(min = 6, max = 64, message = "password length must be between 6 and 64")
    private String password;
}
