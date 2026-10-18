package com.yirancrazy.minimall.platform.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: RoleVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@AllArgsConstructor
public class RoleVO {

    private String roleCode;
    private String description;
    private List<PermissionVO> permissions;
}
