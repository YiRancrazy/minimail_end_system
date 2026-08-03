package com.yirancrazy.minimall.user.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户 User 错误码枚举，定义用户相关错误码
 * @Version: 1.1
 * @DateTime: 2026/08/03
 **/
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
