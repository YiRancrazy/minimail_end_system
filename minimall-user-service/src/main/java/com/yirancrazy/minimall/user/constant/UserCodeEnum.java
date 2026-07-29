package com.yirancrazy.minimall.user.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserCodeEnum implements BaseEnum {
    USER_NOT_FOUND("12001", "USER_NOT_FOUND", "用户不存在");

    private final String code;
    private final String alias;
    private final String message;
}