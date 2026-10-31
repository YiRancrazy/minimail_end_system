package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 角色权限关联持久化对象，映射 t_auth_role_permission；维护角色 ID 与权限 ID 的多对多关系。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_role_permission")
public class AuthRolePermissionPo extends BasePo {
  private Long roleId;

  private Long permissionId;
}
