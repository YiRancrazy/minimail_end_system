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
 * @Description: RolePermissionMapping 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public final class RolePermissionMapping {


    public static final String KEY_PREFIX = "platform:role-permissions:";

    /**
     * 构造 RolePermissionMapping 实例。
     */
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
            PermissionEnum.ROLE_MANAGE,
            PermissionEnum.NOTIFY_BROADCAST,
            PermissionEnum.NOTIFY_VIEW,
            PermissionEnum.PLATFORM_FINANCE
        )
    );


    /**
     * 执行 permissionsOf 方法。
     * @param role role 参数
     * @return 结果列表
     */
    public static Set<PermissionEnum> permissionsOf(RoleEnum role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(MAPPING.getOrDefault(role, Collections.emptySet()));
    }


    /**
     * 执行 hasPermission 方法。
     * @param role role 参数
     * @param permission permission 参数
     * @return 是否处理成功
     */
    public static boolean hasPermission(RoleEnum role, PermissionEnum permission) {
        if (role == null || permission == null) {
            return false;
        }
        return MAPPING.getOrDefault(role, Collections.emptySet()).contains(permission);
    }


    /**
     * 获取相关数据。
     * @param role role 参数
     * @param redisTemplate redisTemplate 参数
     * @return 结果列表
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
     * 执行 parsePermissions 方法。
     * @param json json 参数
     * @return 结果列表
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
     * 更新业务数据。
     * @param role role 参数
     * @param permissionCodes permissionCodes 参数
     * @param redisTemplate redisTemplate 参数
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
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "权限码非法：" + code);
            }
        }
        String json = "[" + permissionCodes.stream()
            .map(c -> "\"" + c + "\"")
            .reduce((a, b) -> a + "," + b).orElse("") + "]";
        redisTemplate.opsForValue().set(KEY_PREFIX + role.getCode(), json);
    }
}
