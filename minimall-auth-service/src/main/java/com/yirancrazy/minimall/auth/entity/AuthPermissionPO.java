package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 权限资源持久化对象，映射 t_auth_permission；描述权限编码、资源地址和启用状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_permission")
public class AuthPermissionPo extends BasePo {
    private String permissionCode;

    private String permissionName;

    private Integer resourceType;

    private String resourcePath;

    private String description;

    private Integer status;
}
