package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证角色持久化对象，映射 t_auth_role；为账号分配角色 ID，并为 JWT 提供角色编码。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_role")
public class AuthRolePo extends BasePo {
  private String roleCode;

  private String roleName;

  private String description;

  private Integer status;
}
