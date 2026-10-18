package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AdminRoleDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class AdminRoleDTO {

    @NotBlank(message = "roleCode cannot be blank")
    @Size(max = 64, message = "roleCode length must be <= 64")
    private String roleCode;
}