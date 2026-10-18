package com.yirancrazy.minimall.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: RoleEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
     * 执行 fromCode 方法。
     * @param code code 参数
     * @return 处理结果
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
