package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data

/**
 * @Author: yirancrazy@gmail.com
 * @Description: LoginDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@NoArgsConstructor
public class LoginDTO {
    @NotBlank
    private String account;

    @NotBlank
    private String password;


    /**
     * 构造 LoginDTO 实例。
     * @param account account 参数
     * @param password password 参数
     */
    public LoginDTO(String account, String password) {
        this.account = account;
        this.password = password;
    }
}
