package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员角色分配 DTO，按角色编码分配角色
 * @Version: 1.0
 * @DateTime: 2026/08/25
 **/
@Data
public class AdminRoleDTO {

    @NotBlank(message = "roleCode cannot be blank")
    @Size(max = 64, message = "roleCode length must be <= 64")
    private String roleCode;
}