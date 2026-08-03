package com.yirancrazy.minimall.platform.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 角色权限视图对象，用于平台端展示角色及其拥有的权限列表。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@AllArgsConstructor
public class RoleVO {

    private String roleCode;
    private String description;
    private List<PermissionVO> permissions;
}
