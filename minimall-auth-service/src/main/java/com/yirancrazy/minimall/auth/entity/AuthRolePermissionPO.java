package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthRolePermission持久化对象，映射t_auth_role_permission表
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_role_permission")
public class AuthRolePermissionPO extends BasePO {
    private Long roleId;

    private Long permissionId;
}
