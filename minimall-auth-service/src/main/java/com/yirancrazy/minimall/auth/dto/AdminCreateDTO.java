package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员创建入参；账号和密码必填，联系方式及角色编码可选，长度上限均为 64。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class AdminCreateDto {

  @NotBlank(message = "account cannot be blank")
  @Size(min = 4, max = 64, message = "account length must be between 4 and 64")
  private String account;

  @NotBlank(message = "password cannot be blank")
  @Size(min = 6, max = 128, message = "password length must be between 6 and 128")
  private String password;

  @Size(max = 64, message = "contact length must be <= 64")
  private String contact;

  @Size(max = 64, message = "roleCode length must be <= 64")
  private String roleCode;
}
