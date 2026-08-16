package com.yirancrazy.minimall.common.constant;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 角色-权限映射表（简化版 RBAC），硬编码各角色拥有的权限集合。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
public final class RolePermissionMapping {

    /**
     * Redis 角色权限覆盖集 key 前缀，完整 key 为 {prefix}{roleCode}，写入与读取共用。
     */
    public static final String KEY_PREFIX = "platform:role-permissions:";

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

    /**
     * 获取角色生效权限集合，优先读 Redis，无则 fallback 到硬编码映射。
     * @param role 角色枚举
     * @param redisTemplate 可为 null，为 null 时直接 fallback
     * @return 权限集合
     */
    public static Set<PermissionEnum> getEffectivePermissions(
            RoleEnum role, StringRedisTemplate redisTemplate) {
        if (role == null) {
            return Collections.emptySet();
        }
        if (redisTemplate != null) {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + role.getCode());
            if (json != null && !json.isEmpty()) {
                return Collections.unmodifiableSet(parsePermissions(json));
            }
        }
        return permissionsOf(role);
    }

    /**
     * 解析 Redis 中存储的权限码 JSON 数组字符串为权限枚举集合，非法权限码自动跳过。
     * @param json 权限码数组字符串，如 ["GOODS_VIEW","GOODS_MANAGE"]
     * @return 权限枚举集合
     */
    public static Set<PermissionEnum> parsePermissions(String json) {
        Set<PermissionEnum> perms = new HashSet<>();
        for (String code : json.replaceAll("[\\[\\]\"]", "").split(",")) {
            try {
                perms.add(PermissionEnum.valueOf(code.trim()));
            }
            catch (IllegalArgumentException ignored) {
                // 忽略脏权限码，避免单条脏数据导致整个覆盖集失效
            }
        }
        return perms;
    }

    /**
     * 更新角色权限映射到 Redis，仅允许修改 MERCHANT 和 PLATFORM 角色。
     * @param role 角色枚举
     * @param permissionCodes 权限编码列表
     * @param redisTemplate Redis 操作模板
     * @throws IllegalArgumentException 尝试修改 USER 角色时
     */
    public static void updatePermissions(
            RoleEnum role, List<String> permissionCodes, StringRedisTemplate redisTemplate) {
        if (role == RoleEnum.USER) {
            throw new IllegalArgumentException("USER role permissions are not modifiable");
        }
        // Validate all permission codes
        for (String code : permissionCodes) {
            try {
                PermissionEnum.valueOf(code);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Invalid permission code: " + code);
            }
        }
        String json = "[" + permissionCodes.stream()
            .map(c -> "\"" + c + "\"")
            .reduce((a, b) -> a + "," + b).orElse("") + "]";
        redisTemplate.opsForValue().set(KEY_PREFIX + role.getCode(), json);
    }
}
