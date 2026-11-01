package com.yirancrazy.minimall.platform.controller.v1;

import com.yirancrazy.minimall.common.annotation.RequirePermission;
import com.yirancrazy.minimall.common.constant.PermissionEnum;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.RolePermissionUpdateDto;
import com.yirancrazy.minimall.platform.service.RolePermissionService;
import com.yirancrazy.minimall.platform.vo.RoleVo;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台角色权限管理接口，提供角色查询、权限查看及权限更新能力.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/roles")
@RequiredArgsConstructor
public class PlatformRoleControllerV1 {

    private final RolePermissionService rolePermissionService;

    /**
     * 查询平台全部角色及其当前生效权限，要求 ROLE_VIEW 权限.
     *
     * @return 角色及生效权限列表
     */
    @GetMapping
    @RequirePermission(PermissionEnum.ROLE_VIEW)
    public Result<List<RoleVo>> listRoles() {
        return Result.success(rolePermissionService.listRoles());
    }

    /**
     * 查询指定角色的生效权限，要求 ROLE_VIEW 权限.
     *
     * @param roleCode 角色编码
     * @return 角色权限视图；角色编码未知时数据为 null
     */
    @GetMapping("/{roleCode}/permissions")
    @RequirePermission(PermissionEnum.ROLE_VIEW)
    public Result<RoleVo> getRolePermissions(@PathVariable String roleCode) {
        return Result.success(rolePermissionService.getRolePermissions(roleCode));
    }

    /**
     * 更新指定角色的权限，要求 ROLE_MANAGE 权限并校验权限码列表.
     *
     * @param roleCode 角色编码
     * @param dto 新权限码列表
     * @return 成功响应，不携带数据
     * @throws com.yirancrazy.minimall.common.exception.BizException 角色不存在、不可修改或更新失败时抛出
     */
    @PostMapping("/{roleCode}/permissions")
    @RequirePermission(PermissionEnum.ROLE_MANAGE)
    public Result<Void> updatePermissions(
            @PathVariable String roleCode, @Valid @RequestBody RolePermissionUpdateDto dto) {
        rolePermissionService.updatePermissions(roleCode, dto.getPermissionCodes());
        return Result.success();
    }
}
