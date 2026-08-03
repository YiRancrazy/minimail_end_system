package com.yirancrazy.minimall.pay.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现状态枚举，定义提现单生命周期状态。
 * @Version: 1.0
 * @DateTime: 2026/08/03
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
}
