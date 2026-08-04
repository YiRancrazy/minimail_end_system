package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 重置密码数据传输对象，通过账号+验证码设置新密码。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordDTO {

    @NotBlank(message = "account cannot be blank")
    private String account;

    @NotBlank(message = "verifyCode cannot be blank")
    @Size(min = 6, max = 6, message = "verifyCode must be 6 digits")
    private String verifyCode;

    @NotBlank(message = "newPassword cannot be blank")
    @Size(min = 6, max = 64, message = "newPassword length must be between 6 and 64")
    private String newPassword;
}
