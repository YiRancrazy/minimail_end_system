package com.yirancrazy.minimall.platform.service;

import com.yirancrazy.minimall.platform.vo.RoleVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台角色权限业务契约，读取角色定义并维护 Redis 中的权限覆盖配置.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface RolePermissionService {

    /**
     * 查询全部平台角色及其当前生效权限.
     *
     * @return 角色及权限列表
     */
    List<RoleVo> listRoles();

    /**
     * 查询指定角色的当前生效权限.
     *
     * @param roleCode 角色编码
     * @return 角色权限视图；编码未知时返回 null
     */
    RoleVo getRolePermissions(String roleCode);

    /**
     * 更新角色的权限覆盖配置并写入 Redis.
     *
     * @param roleCode 角色编码
     * @param permissionCodes 新权限码列表
     * @throws com.yirancrazy.minimall.common.exception.BizException 角色不存在、不可修改或权限无效时抛出
     */
    void updatePermissions(String roleCode, List<String> permissionCodes);
}
