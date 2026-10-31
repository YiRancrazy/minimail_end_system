package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 已认证账号修改密码的入参；需要旧密码和新密码，新密码长度限制为 6 至 64 个字符。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordDto {

  @NotBlank(message = "oldPassword cannot be blank")
  private String oldPassword;

  @NotBlank(message = "newPassword cannot be blank")
  @Size(min = 6, max = 64, message = "newPassword length must be between 6 and 64")
  private String newPassword;
}
