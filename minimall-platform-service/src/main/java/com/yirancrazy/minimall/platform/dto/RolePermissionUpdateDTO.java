package com.yirancrazy.minimall.platform.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 角色权限分配请求体，用于动态修改角色权限映射。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class RolePermissionUpdateDTO {
    @NotEmpty(message = "权限列表不能为空")
    private List<@NotBlank String> permissionCodes;
}
