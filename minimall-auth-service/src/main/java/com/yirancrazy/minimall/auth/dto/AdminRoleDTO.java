package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员角色分配入参；角色编码必填，由服务层解析为持久化角色 ID。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class AdminRoleDto {

  @NotBlank(message = "roleCode cannot be blank")
  @Size(max = 64, message = "roleCode length must be <= 64")
  private String roleCode;
}
