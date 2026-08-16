package com.yirancrazy.minimall.platform.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.common.annotation.RequirePermission;
import com.yirancrazy.minimall.common.constant.PermissionEnum;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.RolePermissionUpdateDTO;
import com.yirancrazy.minimall.platform.service.RolePermissionService;
import com.yirancrazy.minimall.platform.vo.RoleVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台角色权限管理控制器，提供角色与权限的只读查询与权限分配能力。
 * @Version: 1.1
 * @DateTime: 2026/08/13
 **/
@RestController
@RequestMapping("/api/v1/platform/roles")
@RequiredArgsConstructor
public class PlatformRoleControllerV1 {

    private final RolePermissionService rolePermissionService;

    /**
     * 查询全部角色及其权限列表。
     * @return 角色权限列表
     */
    @GetMapping
    @RequirePermission(PermissionEnum.ROLE_VIEW)
    public Result<List<RoleVO>> listRoles() {
        return Result.success(rolePermissionService.listRoles());
    }

    /**
     * 查询指定角色的权限列表。
     * @param roleCode 角色编码
     * @return 角色权限详情；角色不存在时 data 为 null（防止存在性枚举）
     */
    @GetMapping("/{roleCode}/permissions")
    @RequirePermission(PermissionEnum.ROLE_VIEW)
    public Result<RoleVO> getRolePermissions(@PathVariable String roleCode) {
        return Result.success(rolePermissionService.getRolePermissions(roleCode));
    }

    /**
     * 修改指定角色的权限列表，不允许修改 USER 角色权限。
     * 写操作与只读查询权限分离：修改需 ROLE_MANAGE，查询仅需 ROLE_VIEW。
     * @param roleCode 角色编码
     * @param dto 权限分配请求体
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{roleCode}/permissions")
    @RequirePermission(PermissionEnum.ROLE_MANAGE)
    public Result<Void> updatePermissions(@PathVariable String roleCode,
                                          @Valid @RequestBody RolePermissionUpdateDTO dto) {
        rolePermissionService.updatePermissions(roleCode, dto.getPermissionCodes());
        return Result.success();
    }
}
