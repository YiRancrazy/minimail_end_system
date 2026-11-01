package com.yirancrazy.minimall.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 角色权限更新请求体，携带非空的权限码列表.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class RolePermissionUpdateDto {
    @NotEmpty(message = "权限列表不能为空")
    private List<@NotBlank String> permissionCodes;
}
