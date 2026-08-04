package com.yirancrazy.minimall.platform.controller.v1;

import java.util.Arrays;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.constant.RolePermissionMapping;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.RolePermissionUpdateDTO;
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

    private final StringRedisTemplate redisTemplate;

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

    /**
     * 修改指定角色的权限列表，不允许修改 USER 角色权限。
     * @param roleCode 角色编码
     * @param dto 权限分配请求体
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{roleCode}/permissions")
    @RequirePermission(PermissionEnum.ROLE_VIEW)
    public Result<Void> updatePermissions(@PathVariable String roleCode,
                                          @Valid @RequestBody RolePermissionUpdateDTO dto) {
        RoleEnum role = RoleEnum.fromCode(roleCode);
        if (role == null) {
            return Result.fail("12010", "角色不存在");
        }
        try {
            RolePermissionMapping.updatePermissions(role, dto.getPermissionCodes(), redisTemplate);
        }
        catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not modifiable")) {
                return Result.fail("12010", "该角色不允许修改权限");
            }
            return Result.fail("12011", e.getMessage());
        }
        return Result.success(null);
    }

    private RoleVO toRoleVO(RoleEnum role) {
        List<PermissionVO> permissions = RolePermissionMapping.getEffectivePermissions(role, redisTemplate).stream()
            .map(p -> new PermissionVO(p.getCode(), p.getDescription()))
            .toList();
        return new RoleVO(role.getCode(), role.getDescription(), permissions);
    }
}
