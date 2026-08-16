package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺状态枚举，对齐 t_merch_shop.status 持久化 int code；DTO 入参沿用 alias 字符串（ACTIVE/INACTIVE/SUSPENDED）由服务层转换。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
@Getter
@AllArgsConstructor
public enum ShopStatusEnum implements BaseEnum {
    ACTIVE(1, "ACTIVE", "营业中"),
    INACTIVE(2, "INACTIVE", "已停业"),
    SUSPENDED(3, "SUSPENDED", "已冻结");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 返回 int 形态状态码，用于持久化。
     * @return 状态码
     */
    public int intCode() {
        return code;
    }

    /**
     * 将持久化状态码转换为枚举，非法时返回 null（由调用方决定错误语义）。
     * @param code 持久化状态码
     * @return 对应枚举；未知状态码返回 null
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
     * 将入参 alias 字符串转换为枚举，非法时返回 null（由服务层抛参数错误）。
     * @param alias DTO 传入的状态字符串
     * @return 对应枚举；未知状态返回 null
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
