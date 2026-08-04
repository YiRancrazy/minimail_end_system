package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthRole持久化对象，映射t_auth_role表
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_auth_role")
public class AuthRolePO extends BasePO {
    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;
}
