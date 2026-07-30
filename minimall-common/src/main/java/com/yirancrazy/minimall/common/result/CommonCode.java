package com.yirancrazy.minimall.common.result;

/**
 * 通用响应码常量类，定义成功码 00000、系统错误码 20000 和参数校验失败码 20001 等全局公共返回码。
 */
public final class CommonCode
{
    private CommonCode() {}

    public static final String SUCCESS = "00000";
    public static final String SYS_ERROR = "20000";
    public static final String PARAM_INVALID = "20001";
}