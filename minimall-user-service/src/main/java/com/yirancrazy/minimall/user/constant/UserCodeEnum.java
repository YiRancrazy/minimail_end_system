package com.yirancrazy.minimall.user.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
* 用户服务错误码枚举，定义用户不存在等领域业务错误及其提示信息。
 */
@Getter
@AllArgsConstructor
public enum UserCodeEnum implements BaseEnum {
    USER_NOT_FOUND("12001", "USER_NOT_FOUND", "用户不存在");

    private final String code;
    private final String alias;
    private final String message;
}