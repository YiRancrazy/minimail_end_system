package com.yirancrazy.minimall.platform.controller.v1;

import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.common.annotation.RequirePermission;
import com.yirancrazy.minimall.common.constant.PermissionEnum;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.constant.RolePermissionMapping;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.vo.PermissionVO;
import com.yirancrazy.minimall.platform.vo.RoleVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台角色权限管理控制器，提供角色与权限的只读查询能力。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@RestController
@RequestMapping("/api/v1/platform/roles")
@RequiredArgsConstructor
public class PlatformRoleControllerV1 {

    /**
     * 查询全部角色及其权限列表。
     * @return 角色权限列表
     */
    @GetMapping
    @RequirePermission(PermissionEnum.ROLE_VIEW)
    public Result<List<RoleVO>> listRoles() {
        List<RoleVO> roles = Arrays.stream(RoleEnum.values())
            .map(this::toRoleVO)
            .toList();
        return Result.success(roles);
    }

    /**
     * 查询指定角色的权限列表。
     * @param roleCode 角色编码
     * @return 角色权限详情
     */
    @GetMapping("/{roleCode}/permissions")
    @RequirePermission(PermissionEnum.ROLE_VIEW)
    public Result<RoleVO> getRolePermissions(@PathVariable String roleCode) {
        RoleEnum role = RoleEnum.fromCode(roleCode);
        if (role == null) {
            return Result.success(null);
        }
        return Result.success(toRoleVO(role));
    }

    private RoleVO toRoleVO(RoleEnum role) {
        List<PermissionVO> permissions = RolePermissionMapping.permissionsOf(role).stream()
            .map(p -> new PermissionVO(p.getCode(), p.getDescription()))
            .toList();
        return new RoleVO(role.getCode(), role.getDescription(), permissions);
    }
}
