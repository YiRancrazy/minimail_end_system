package com.yirancrazy.minimall.auth.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证Auth错误码枚举，定义认证相关错误码
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
@Getter
@AllArgsConstructor
public enum AuthCodeEnum implements BaseEnum {
    USER_NOT_FOUND("14001", "USER_NOT_FOUND", "用户不存在"),
    PWD_INVALID("14002", "PWD_INVALID", "密码错误"),
    TOKEN_INVALID("14003", "TOKEN_INVALID", "Token 无效"),
    TOKEN_EXPIRED("14004", "TOKEN_EXPIRED", "Token 已过期"),
    USER_EXISTS("14005", "USER_EXISTS", "用户已存在"),
    REFRESH_TOKEN_INVALID("14006", "REFRESH_TOKEN_INVALID", "Refresh Token 无效"),
    ACCESS_DENIED("14007", "ACCESS_DENIED", "无访问权限"),
    VERIFY_CODE_INVALID("14008", "VERIFY_CODE_INVALID", "验证码无效或已过期"),
    ACCOUNT_DISABLED("14009", "ACCOUNT_DISABLED", "账号已禁用"),
    ACCOUNT_ROLE_MISMATCH("14010", "ACCOUNT_ROLE_MISMATCH", "账号角色不匹配"),
    MERCHANT_NOT_FOUND("14011", "MERCHANT_NOT_FOUND", "商家账号不存在"),
    ADMIN_USERNAME_EXISTS("14012", "ADMIN_USERNAME_EXISTS", "管理员用户名已存在"),
    CANNOT_DELETE_SELF("14013", "CANNOT_DELETE_SELF", "不能删除自己"),
    MERCHANT_EXISTS("14014", "MERCHANT_EXISTS", "商家账号已存在"),
    RESET_CODE_TOO_FREQUENT("14015", "RESET_CODE_TOO_FREQUENT", "验证码发送过于频繁"),
    VERIFY_CODE_ATTEMPT_EXCEEDED("14016", "VERIFY_CODE_ATTEMPT_EXCEEDED", "验证码错误次数过多，请重新获取");

    private final String code;
    private final String alias;
    private final String message;
}
