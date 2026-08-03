package com.yirancrazy.minimall.common.constant;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 角色-权限映射表（简化版 RBAC），硬编码各角色拥有的权限集合。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
public final class RolePermissionMapping {

    private RolePermissionMapping() {
    }

    private static final Map<RoleEnum, Set<PermissionEnum>> MAPPING = Map.of(
        RoleEnum.USER, EnumSet.of(
            PermissionEnum.GOODS_VIEW,
            PermissionEnum.CART_MANAGE,
            PermissionEnum.ORDER_CREATE,
            PermissionEnum.ORDER_VIEW,
            PermissionEnum.ORDER_CANCEL,
            PermissionEnum.PAY_VIEW,
            PermissionEnum.NOTIFY_VIEW,
            PermissionEnum.PROFILE_MANAGE
        ),
        RoleEnum.MERCHANT, EnumSet.of(
            PermissionEnum.GOODS_VIEW,
            PermissionEnum.GOODS_MANAGE,
            PermissionEnum.STOCK_MANAGE,
            PermissionEnum.ORDER_VIEW,
            PermissionEnum.ORDER_SHIP,
            PermissionEnum.PAY_VIEW,
            PermissionEnum.NOTIFY_VIEW,
            PermissionEnum.QUALIFICATION_SUBMIT
        ),
        RoleEnum.PLATFORM, EnumSet.of(
            PermissionEnum.GOODS_VIEW,
            PermissionEnum.MERCHANT_AUDIT,
            PermissionEnum.GOODS_AUDIT,
            PermissionEnum.ORDER_VIEW_ALL,
            PermissionEnum.ORDER_STATISTICS,
            PermissionEnum.ORDER_DELETE,
            PermissionEnum.STOCK_VIEW_ALL,
            PermissionEnum.PAY_MANAGE,
            PermissionEnum.REFUND_AUDIT,
            PermissionEnum.ROLE_VIEW,
            PermissionEnum.NOTIFY_BROADCAST,
            PermissionEnum.NOTIFY_VIEW,
            PermissionEnum.PLATFORM_FINANCE
        )
    );

    /**
     * 获取指定角色的全部权限集合（不可变）。
     * @param role 角色枚举
     * @return 权限集合，未知角色返回空集
     */
    public static Set<PermissionEnum> permissionsOf(RoleEnum role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(MAPPING.getOrDefault(role, Collections.emptySet()));
    }

    /**
     * 判断角色是否拥有指定权限。
     * @param role 角色枚举
     * @param permission 权限枚举
     * @return true 表示拥有
     */
    public static boolean hasPermission(RoleEnum role, PermissionEnum permission) {
        if (role == null || permission == null) {
            return false;
        }
        return MAPPING.getOrDefault(role, Collections.emptySet()).contains(permission);
    }
}
