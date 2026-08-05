package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现状态枚举，对齐 t_merchant_withdraw.status。本期仅流转 PENDING→APPROVED/REJECTED。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Getter
@AllArgsConstructor
public enum WithdrawStatusEnum implements BaseEnum {
    PENDING(1, "PENDING", "待审核"),
    APPROVED(2, "APPROVED", "已通过"),
    REJECTED(3, "REJECTED", "已拒绝"),
    PAID(4, "PAID", "已打款");

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
     * 将 int 状态码转换为枚举，非法时抛 IllegalArgumentException。
     * @param code 持久化状态码
     * @return 对应枚举
     */
    public static WithdrawStatusEnum fromCode(int code) {
        for (WithdrawStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        throw new IllegalArgumentException("unknown withdraw status code: " + code);
    }
}
