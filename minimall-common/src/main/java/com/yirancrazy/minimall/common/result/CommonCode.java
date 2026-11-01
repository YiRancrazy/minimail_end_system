package com.yirancrazy.minimall.common.result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 跨业务模块共享的系统级、参数级和幂等控制错误码。
 * 服务专属错误码应留在各自模块，该类不承载具体业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public final class CommonCode {
    /** 常量容器禁止实例化。 */
    private CommonCode() {}

    public static final String SUCCESS = "00000";
    public static final String SYS_ERROR = "20000";
    public static final String PARAM_INVALID = "20001";
    public static final String FORBIDDEN = "20003";

    public static final String IDEMPOTENT_KEY_REUSED = "13002";
    public static final String IDEMPOTENT_KEY_NOT_EXIST = "13003";
}
