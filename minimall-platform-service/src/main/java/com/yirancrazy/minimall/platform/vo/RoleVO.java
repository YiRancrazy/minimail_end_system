package com.yirancrazy.minimall.platform.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台角色及其当前生效权限的聚合视图.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
public class RoleVo {

  private String roleCode;
  private String description;
  private List<PermissionVo> permissions;
}
