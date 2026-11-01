package com.yirancrazy.minimall.platform.service.impl;

import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.constant.RolePermissionMapping;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.service.RolePermissionService;
import com.yirancrazy.minimall.platform.vo.PermissionVo;
import com.yirancrazy.minimall.platform.vo.RoleVo;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 角色权限实现，基于角色枚举读取权限，并将角色权限覆盖配置持久化到 Redis.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 遍历系统角色并组装各自当前生效的权限.
     *
     * @return USER、MERCHANT、PLATFORM 角色的权限视图列表
     */
    @Override
    public List<RoleVo> listRoles() {
        return Arrays.stream(RoleEnum.values()).map(this::toRoleVo).toList();
    }

    /**
     * 按角色编码查询当前生效权限.
     *
     * @param roleCode 角色编码
     * @return 角色权限视图；编码为空或未知时返回 null
     */
    @Override
    public RoleVo getRolePermissions(String roleCode) {
        RoleEnum role = RoleEnum.fromCode(roleCode);
        return role == null ? null : toRoleVo(role);
    }

    /**
     * 校验角色及权限码后更新角色权限覆盖配置.
     *
     * <p>权限配置由 RolePermissionMapping 写入 Redis，USER 角色不可修改；权限码无效时统一转换为更新失败。
     *
     * @param roleCode 角色编码
     * @param permissionCodes 新权限码列表
     * @throws BizException 角色不存在、不允许修改或权限更新失败时抛出
     */
    public void updatePermissions(String roleCode, List<String> permissionCodes) {
        RoleEnum role = RoleEnum.fromCode(roleCode);
        if (role == null) {
            throw new BizException(PlatformCodeEnum.ROLE_NOT_FOUND);
        }
        try {
            RolePermissionMapping.updatePermissions(role, permissionCodes, redisTemplate);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("不可修改")) {
                throw new BizException(PlatformCodeEnum.ROLE_PERMISSION_NOT_MODIFIABLE);
            }
            throw new BizException(PlatformCodeEnum.ROLE_PERMISSION_UPDATE_FAILED);
        }
    }

    /**
     * 组装角色编码、描述及 Redis 覆盖后的生效权限.
     *
     * @param role 角色枚举
     * @return 角色及权限视图
     */
    private RoleVo toRoleVo(RoleEnum role) {
        List<PermissionVo> permissions =
                RolePermissionMapping.getEffectivePermissions(role, redisTemplate).stream()
                        .map(p -> new PermissionVo(p.getCode(), p.getDescription()))
                        .toList();
        return new RoleVo(role.getCode(), role.getDescription(), permissions);
    }
}
