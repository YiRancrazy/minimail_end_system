package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Login数据传输对象，用于Login相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@Data

@NoArgsConstructor
public class LoginDTO {
    @NotBlank
    private String account;

    @NotBlank
    private String password;

    /**
     * 便利构造函数。
     * @param account 登录账号
     * @param password 密码
     */
    public LoginDTO(String account, String password) {
        this.account = account;
        this.password = password;
    }
}
