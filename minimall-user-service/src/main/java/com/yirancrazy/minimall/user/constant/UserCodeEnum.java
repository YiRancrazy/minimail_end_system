package com.yirancrazy.minimall.user.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户User错误码枚举，定义用户相关错误码
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Getter

@AllArgsConstructor
public enum UserCodeEnum implements BaseEnum {
    USER_NOT_FOUND("12001", "USER_NOT_FOUND", "用户不存在"),
    PARAM_INVALID("12002", "PARAM_INVALID", "参数非法");

    private final String code;
    private final String alias;
    private final String message;
}