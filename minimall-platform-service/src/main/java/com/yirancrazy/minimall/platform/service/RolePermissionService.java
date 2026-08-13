package com.yirancrazy.minimall.platform.service;

import java.util.List;
import com.yirancrazy.minimall.platform.vo.RoleVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台角色权限服务，负责角色权限的查询与分配（Redis 读写 + 校验）。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
public interface RolePermissionService {

    /**
     * 查询全部角色及其权限列表。
     * @return 角色权限列表
     */
    List<RoleVO> listRoles();

    /**
     * 查询指定角色的权限详情。
     * @param roleCode 角色编码
     * @return 角色权限详情；角色不存在时返回 null（防止存在性枚举）
     */
    RoleVO getRolePermissions(String roleCode);

    /**
     * 更新指定角色的权限列表。
     * @param roleCode 角色编码
     * @param permissionCodes 权限编码列表
     * @throws BizException 角色不存在或该角色权限不可修改时
     */
    void updatePermissions(String roleCode, List<String> permissionCodes);
}
