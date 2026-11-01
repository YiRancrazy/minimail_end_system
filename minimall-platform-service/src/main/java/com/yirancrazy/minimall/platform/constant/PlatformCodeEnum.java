package com.yirancrazy.minimall.platform.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台业务错误码，覆盖商家、用户查询及角色权限更新场景.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum PlatformCodeEnum implements BaseEnum {
    PLATFORM_MERCHANT_NOT_FOUND("20010", "PLATFORM_MERCHANT_NOT_FOUND", "商家不存在"),
    PLATFORM_USER_NOT_FOUND("20011", "PLATFORM_USER_NOT_FOUND", "用户不存在"),
    PLATFORM_AUDIT_STATUS_INVALID("20012", "PLATFORM_AUDIT_STATUS_INVALID", "审核状态非法"),
    // 角色权限域错误码：12010~12012 为历史遗留段位，待错误码统一对齐（B1）时整体迁移
    ROLE_NOT_FOUND("12010", "ROLE_NOT_FOUND", "角色不存在"),
    ROLE_PERMISSION_NOT_MODIFIABLE("12011", "ROLE_PERMISSION_NOT_MODIFIABLE", "该角色不允许修改权限"),
    ROLE_PERMISSION_UPDATE_FAILED("12012", "ROLE_PERMISSION_UPDATE_FAILED", "角色权限更新失败");

    private final String code;
    private final String alias;
    private final String message;
}
