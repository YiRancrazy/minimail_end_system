package com.yirancrazy.minimall.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 角色下生效权限的编码与说明视图.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
public class PermissionVo {

  private String permissionCode;
  private String description;
}
