package com.yirancrazy.minimall.common.result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 响应码契约接口，约定业务码枚举必须提供的编码、别名和提示消息三个字段，供 Result 与 BizException 统一读取。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface ResultCode {
    String getCode();
    String getAlias();
    String getMessage();
}