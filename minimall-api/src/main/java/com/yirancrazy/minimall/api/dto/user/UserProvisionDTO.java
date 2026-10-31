package com.yirancrazy.minimall.api.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证成功后同步创建用户业务档案的请求.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProvisionDto {

  @NotNull(message = "userId cannot be null")
  private Long userId;
}
