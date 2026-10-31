package com.yirancrazy.minimall.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 系统角色定义，区分 C 端用户、商家和平台管理员，并维护稳定的角色编码。
 * 角色编码来自受信任的 X-User-Role 请求头，用于选择权限集合。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {
  USER("USER", "C端用户"),
  MERCHANT("MERCHANT", "商家"),
  PLATFORM("PLATFORM", "平台管理员");

  private final String code;
  private final String description;

  /**
   * 按角色编码精确查找枚举值。
   *
   * @param code 角色编码，可为 null
   * @return 匹配的角色；输入为 null 或编码未知时返回 null
   */
  public static RoleEnum fromCode(String code) {
    if (code == null) {
      return null;
    }
    for (RoleEnum role : values()) {
      if (role.code.equals(code)) {
        return role;
      }
    }
    return null;
  }
}
