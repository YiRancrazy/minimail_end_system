package com.yirancrazy.minimall.common.result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务码枚举的统一契约，要求同时提供机器码、稳定别名和用户可读消息。
 * 该接口用于异常、响应和枚举之间的解耦，不规定码段分配规则。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface ResultCode {
    /**
     * 返回用于接口响应的稳定业务码。
     *
     * @return 业务码字符串
     */
    String getCode();

    /**
     * 返回便于日志检索和错误定位的英文别名。
     *
     * @return 错误别名
     */
    String getAlias();

    /**
     * 返回面向调用方的中文错误消息。
     *
     * @return 错误消息
     */
    String getMessage();
}
