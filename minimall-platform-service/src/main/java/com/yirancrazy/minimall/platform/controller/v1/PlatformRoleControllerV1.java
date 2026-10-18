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
 * @Description: PlatformRoleControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/roles")
@RequiredArgsConstructor
public class PlatformRoleControllerV1 {

    private final RolePermissionService rolePermissionService;


    /**
     * 查询列表数据。
     * @return 统一响应结果
     */
    @GetMapping
    @RequirePermission(PermissionEnum.ROLE_VIEW)
    public Result<List<RoleVO>> listRoles() {
        return Result.success(rolePermissionService.listRoles());
    }


    /**
     * 获取相关数据。
     * @param roleCode roleCode 参数
     * @return 统一响应结果
     */
    @GetMapping("/{roleCode}/permissions")
    @RequirePermission(PermissionEnum.ROLE_VIEW)
    public Result<RoleVO> getRolePermissions(@PathVariable String roleCode) {
        return Result.success(rolePermissionService.getRolePermissions(roleCode));
    }


    /**
     * 更新业务数据。
     * @param roleCode roleCode 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/{roleCode}/permissions")
    @RequirePermission(PermissionEnum.ROLE_MANAGE)
    public Result<Void> updatePermissions(@PathVariable String roleCode,
                                          @Valid @RequestBody RolePermissionUpdateDTO dto) {
        rolePermissionService.updatePermissions(roleCode, dto.getPermissionCodes());
        return Result.success();
    }
}
