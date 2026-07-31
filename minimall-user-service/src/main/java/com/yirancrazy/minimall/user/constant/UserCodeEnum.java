package com.yirancrazy.minimall.user.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserCodeEnum description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public enum UserCodeEnum implements BaseEnum {
    USER_NOT_FOUND("12001", "USER_NOT_FOUND", "用户不存在");

    private final String code;
    private final String alias;
    private final String message;
}