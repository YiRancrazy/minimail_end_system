package com.yirancrazy.minimall.pay.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayCodeEnum implements BaseEnum {
    PAY_NOT_FOUND("17001", "PAY_NOT_FOUND", "支付单不存在");

    private final String code;
    private final String alias;
    private final String message;
}