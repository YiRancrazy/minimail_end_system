package com.yirancrazy.minimall.merchant.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺生命周期状态枚举；定义营业中、停业和冻结的持久化 code 与对外 alias，并提供 code、alias 转换。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum ShopStatusEnum implements BaseEnum {
    ACTIVE(1, "ACTIVE", "营业中"),
    INACTIVE(2, "INACTIVE", "已停业"),
    SUSPENDED(3, "SUSPENDED", "已冻结");

    private final int code;
    private final String alias;
    private final String message;

    /**
     * 将店铺状态整数 code 转换为 BaseEnum 约定的字符串形式。
     *
     * @return 1（营业中）、2（已停业）或 3（已冻结）的字符串值。
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 返回用于数据库持久化的店铺状态整数 code。
     *
     * @return 1（营业中）、2（已停业）或 3（已冻结）。
     */
    public int intCode() {
        return code;
    }

    /**
     * 按持久化 code 查找店铺状态。
     *
     * @param code 待匹配的整数状态码
     * @return 匹配的枚举；未匹配时返回 null。
     */
    public static ShopStatusEnum fromCode(int code) {
        for (ShopStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    /**
     * 按对外 alias 查找店铺状态，匹配区分大小写。
     *
     * @param alias 待匹配的状态别名
     * @return 匹配的枚举；未匹配或 alias 为 null 时返回 null。
     */
    public static ShopStatusEnum fromAlias(String alias) {
        for (ShopStatusEnum e : values()) {
            if (e.alias.equals(alias)) {
                return e;
            }
        }
        return null;
    }
}
