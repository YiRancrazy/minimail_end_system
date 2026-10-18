package com.yirancrazy.minimall.common.result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommonCode 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public final class CommonCode {
    /**
     * 构造 CommonCode 实例。
     */
    private CommonCode() {}

    public static final String SUCCESS = "00000";
    public static final String SYS_ERROR = "20000";
    public static final String PARAM_INVALID = "20001";
    public static final String FORBIDDEN = "20003";

    public static final String IDEMPOTENT_KEY_REUSED = "13002";
}
