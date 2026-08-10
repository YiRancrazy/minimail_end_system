package com.yirancrazy.minimall.platform.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台错误码枚举，code 段位 200xx 为平台业务码
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Getter
@AllArgsConstructor
public enum PlatformCodeEnum implements BaseEnum {
    PLATFORM_MERCHANT_NOT_FOUND("20010", "PLATFORM_MERCHANT_NOT_FOUND", "商家不存在"),
    PLATFORM_USER_NOT_FOUND("20011", "PLATFORM_USER_NOT_FOUND", "用户不存在"),
    PLATFORM_AUDIT_STATUS_INVALID("20012", "PLATFORM_AUDIT_STATUS_INVALID", "审核状态非法");

    private final String code;
    private final String alias;
    private final String message;
}
