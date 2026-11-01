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
 * @Description: 维护角色与权限的内置映射，并支持从 Redis 读取平台下发的权限覆盖配置。
 * 默认映射对外只读；用户角色权限固定不可通过管理接口修改。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public final class RolePermissionMapping {

    public static final String KEY_PREFIX = "platform:role-permissions:";

    /** 工具类禁止实例化。 */
    private RolePermissionMapping() {}

    private static final Map<RoleEnum, Set<PermissionEnum>> MAPPING =
            Map.of(
                    RoleEnum.USER,
                            EnumSet.of(
                                    PermissionEnum.GOODS_VIEW,
                                    PermissionEnum.CART_MANAGE,
                                    PermissionEnum.ORDER_CREATE,
                                    PermissionEnum.ORDER_VIEW,
                                    PermissionEnum.ORDER_CANCEL,
                                    PermissionEnum.PAY_VIEW,
                                    PermissionEnum.NOTIFY_VIEW,
                                    PermissionEnum.PROFILE_MANAGE),
                    RoleEnum.MERCHANT,
                            EnumSet.of(
                                    PermissionEnum.GOODS_VIEW,
                                    PermissionEnum.GOODS_MANAGE,
                                    PermissionEnum.STOCK_MANAGE,
                                    PermissionEnum.ORDER_VIEW,
                                    PermissionEnum.ORDER_SHIP,
                                    PermissionEnum.PAY_VIEW,
                                    PermissionEnum.NOTIFY_VIEW,
                                    PermissionEnum.QUALIFICATION_SUBMIT),
                    RoleEnum.PLATFORM,
                            EnumSet.of(
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
                                    PermissionEnum.ROLE_MANAGE,
                                    PermissionEnum.NOTIFY_BROADCAST,
                                    PermissionEnum.NOTIFY_VIEW,
                                    PermissionEnum.PLATFORM_FINANCE));

    /**
     * 返回角色在代码内置映射中的默认权限。
     *
     * @param role 目标角色，可为 null
     * @return 不可修改的权限集合；角色为 null 时返回空集合
     */
    public static Set<PermissionEnum> permissionsOf(RoleEnum role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(MAPPING.getOrDefault(role, Collections.emptySet()));
    }

    /**
     * 判断角色在默认映射中是否拥有指定权限。
     *
     * @param role 目标角色
     * @param permission 待检查权限
     * @return 角色和权限均非空且映射包含该权限时返回 true
     */
    public static boolean hasPermission(RoleEnum role, PermissionEnum permission) {
        if (role == null || permission == null) {
            return false;
        }
        return MAPPING.getOrDefault(role, Collections.emptySet()).contains(permission);
    }

    /**
     * 获取角色的有效权限：Redis 存在非空覆盖配置时优先使用，否则回退到内置映射。
     *
     * @param role 目标角色，可为 null
     * @param redisTemplate 权限覆盖配置存储；为 null 时直接使用内置映射
     * @return 不可修改的有效权限集合
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
     * 将 JSON 数组风格的权限码字符串解析为枚举集合，无法识别的权限码会被忽略。
     *
     * @param json 例如 {@code ["ORDER_VIEW","PAY_VIEW"]} 的权限码字符串
     * @return 成功识别的权限集合
     */
    public static Set<PermissionEnum> parsePermissions(String json) {
        Set<PermissionEnum> perms = new HashSet<>();
        for (String code : json.replaceAll("[\\[\\]\"]", "").split(",")) {
            try {
                perms.add(PermissionEnum.valueOf(code.trim()));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid permission codes
            }
        }
        return perms;
    }

    /**
     * 校验权限码后覆盖指定角色在 Redis 中的权限配置，不接受直接修改用户角色权限。
     *
     * @param role 待更新的商家或平台角色
     * @param permissionCodes 新权限码列表，所有编码都必须存在
     * @param redisTemplate 权限覆盖配置存储
     * @throws IllegalArgumentException 角色为用户角色、权限码非法或 Redis 模板缺失时抛出
     */
    public static void updatePermissions(
            RoleEnum role, List<String> permissionCodes, StringRedisTemplate redisTemplate) {
        if (role == RoleEnum.USER) {
            throw new IllegalArgumentException("USER 角色权限不可修改");
        }
        // 校验所有权限码
        for (String code : permissionCodes) {
            try {
                PermissionEnum.valueOf(code);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("权限码非法：" + code);
            }
        }
        String json =
                "["
                        + permissionCodes.stream()
                                .map(c -> "\"" + c + "\"")
                                .reduce((a, b) -> a + "," + b)
                                .orElse("")
                        + "]";
        redisTemplate.opsForValue().set(KEY_PREFIX + role.getCode(), json);
    }
}
