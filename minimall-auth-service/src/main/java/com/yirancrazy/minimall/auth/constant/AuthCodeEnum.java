package com.yirancrazy.minimall.auth.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthCodeEnum implements BaseEnum {
    TOKEN_INVALID("14003", "TOKEN_INVALID", "Token 无效");

    private final String code;
    private final String alias;
    private final String message;
}