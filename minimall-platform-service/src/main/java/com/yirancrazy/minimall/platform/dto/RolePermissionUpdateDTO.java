package com.yirancrazy.minimall.platform.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: RolePermissionUpdateDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class RolePermissionUpdateDTO {
    @NotEmpty(message = "权限列表不能为空")
    private List<@NotBlank String> permissionCodes;
}
