package com.yirancrazy.minimall.user.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户域业务错误码，覆盖用户、参数、收货地址数量和收货地址不存在等失败场景.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum UserCodeEnum implements BaseEnum {
    USER_NOT_FOUND("12001", "USER_NOT_FOUND", "用户不存在"),
    PARAM_INVALID("12002", "PARAM_INVALID", "参数非法"),
    ADDRESS_LIMIT_EXCEEDED("12003", "ADDRESS_LIMIT_EXCEEDED", "收货地址数量超限"),
    ADDRESS_NOT_FOUND("12004", "ADDRESS_NOT_FOUND", "收货地址不存在");

    private final String code;
    private final String alias;
    private final String message;
}
