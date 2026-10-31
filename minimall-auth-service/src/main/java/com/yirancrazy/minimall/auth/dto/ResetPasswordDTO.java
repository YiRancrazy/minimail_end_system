package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户验证重置码并设置新密码的入参；验证码固定 6 位，新密码长度限制为 6 至 64 个字符。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordDto {

  @NotBlank(message = "account cannot be blank")
  private String account;

  @NotBlank(message = "verifyCode cannot be blank")
  @Size(min = 6, max = 6, message = "verifyCode must be 6 digits")
  private String verifyCode;

  @NotBlank(message = "newPassword cannot be blank")
  @Size(min = 6, max = 64, message = "newPassword length must be between 6 and 64")
  private String newPassword;
}
