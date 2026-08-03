package com.yirancrazy.minimall.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 权限视图对象，用于平台端展示权限编码与描述。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@AllArgsConstructor
public class PermissionVO {

    private String permissionCode;
    private String description;
}
