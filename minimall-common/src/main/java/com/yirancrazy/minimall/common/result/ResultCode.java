package com.yirancrazy.minimall.common.result;

/**
 * 响应码契约接口，约定业务码枚举必须提供的编码、别名和提示消息三个字段。
 */
public interface ResultCode
{
    String getCode();
    String getAlias();
    String getMessage();
}