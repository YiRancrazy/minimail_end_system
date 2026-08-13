package com.yirancrazy.minimall.common.result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 全局公共返回码常量，定义成功码、系统错误码、参数校验码、鉴权码与幂等码。
 * @Version: 1.1
 * @DateTime: 2026/08/13
 */
public final class CommonCode {
    private CommonCode() {}

    public static final String SUCCESS = "00000";
    public static final String SYS_ERROR = "20000";
    public static final String PARAM_INVALID = "20001";
    public static final String FORBIDDEN = "20003";
    /** 幂等键重复：同一 X-Idempotency-Key 在 TTL 内重复提交。13000/13002 属 IDEM 预留段，待 B1 统一对齐 */
    public static final String IDEMPOTENT_KEY_REUSED = "13002";
}
