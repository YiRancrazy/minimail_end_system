package com.yirancrazy.minimall.auth.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthCodeEnum implements BaseEnum {
    USER_NOT_FOUND("14001", "USER_NOT_FOUND", "用户不存在"),
    PWD_INVALID("14002", "PWD_INVALID", "密码错误"),
    TOKEN_INVALID("14003", "TOKEN_INVALID", "Token 无效"),
    TOKEN_EXPIRED("14004", "TOKEN_EXPIRED", "Token 已过期"),
    USER_EXISTS("14005", "USER_EXISTS", "用户已存在");

    private final String code;
    private final String alias;
    private final String message;
}