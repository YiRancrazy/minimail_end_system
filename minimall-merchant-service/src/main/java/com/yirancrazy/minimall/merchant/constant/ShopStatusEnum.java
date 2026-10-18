package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopStatusEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }


    /**
     * 执行 intCode 方法。
     * @return 业务数值
     */
    public int intCode() {
        return code;
    }


    /**
     * 执行 fromCode 方法。
     * @param code code 参数
     * @return 处理结果
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
     * 执行 fromAlias 方法。
     * @param alias alias 参数
     * @return 处理结果
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
