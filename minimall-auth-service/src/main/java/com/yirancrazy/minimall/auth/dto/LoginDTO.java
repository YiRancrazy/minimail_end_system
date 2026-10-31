package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户、商家和平台登录接口共用的账号密码入参；字段约束由 Bean Validation 校验。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
public class LoginDto {
  @NotBlank private String account;

  @NotBlank private String password;

  /**
   * 创建包含登录凭据的入参对象，供服务测试和直接构造场景使用。
   *
   * @param account 登录账号
   * @param password 明文登录密码，仅在认证校验期间使用
   */
  public LoginDto(String account, String password) {
    this.account = account;
    this.password = password;
  }
}
