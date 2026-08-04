package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthPermission持久化对象，映射t_auth_permission表
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_permission")
public class AuthPermissionPO extends BasePO {
    private String permissionCode;

    private String permissionName;

    private Integer resourceType;

    private String resourcePath;

    private String description;

    private Integer status;
}
