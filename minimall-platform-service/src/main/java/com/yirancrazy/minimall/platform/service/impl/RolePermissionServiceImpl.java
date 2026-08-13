package com.yirancrazy.minimall.platform.service.impl;

import java.util.Arrays;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.constant.RolePermissionMapping;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.service.RolePermissionService;
import com.yirancrazy.minimall.platform.vo.PermissionVO;
import com.yirancrazy.minimall.platform.vo.RoleVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台角色权限服务实现，基于硬编码映射 + Redis 覆盖存储，USER 角色权限不可修改。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public List<RoleVO> listRoles() {
        return Arrays.stream(RoleEnum.values())
            .map(this::toRoleVO)
            .toList();
    }

    @Override
    public RoleVO getRolePermissions(String roleCode) {
        RoleEnum role = RoleEnum.fromCode(roleCode);
        return role == null ? null : toRoleVO(role);
    }

    @Override
    public void updatePermissions(String roleCode, List<String> permissionCodes) {
        RoleEnum role = RoleEnum.fromCode(roleCode);
        if (role == null) {
            throw new BizException(PlatformCodeEnum.ROLE_NOT_FOUND);
        }
        try {
            RolePermissionMapping.updatePermissions(role, permissionCodes, redisTemplate);
        }
        catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not modifiable")) {
                throw new BizException(PlatformCodeEnum.ROLE_PERMISSION_NOT_MODIFIABLE);
            }
            throw new BizException(PlatformCodeEnum.ROLE_PERMISSION_UPDATE_FAILED);
        }
    }

    private RoleVO toRoleVO(RoleEnum role) {
        List<PermissionVO> permissions = RolePermissionMapping.getEffectivePermissions(role, redisTemplate).stream()
            .map(p -> new PermissionVO(p.getCode(), p.getDescription()))
            .toList();
        return new RoleVO(role.getCode(), role.getDescription(), permissions);
    }
}
