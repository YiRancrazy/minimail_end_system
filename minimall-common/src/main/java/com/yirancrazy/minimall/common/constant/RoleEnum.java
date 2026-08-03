package com.yirancrazy.minimall.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 系统角色枚举，与 JWT role claim 及网关 X-User-Role 头保持一致。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Getter
@AllArgsConstructor
public enum RoleEnum {

    USER("USER", "C端用户"),
    MERCHANT("MERCHANT", "商家"),
    PLATFORM("PLATFORM", "平台管理员");

    private final String code;
    private final String description;

    /**
     * 根据 string code 解析为枚举，匹配失败时返回 null。
     * @param code 角色字符串
     * @return 匹配到的角色枚举，或 null
     */
    public static RoleEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (RoleEnum role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }
}
