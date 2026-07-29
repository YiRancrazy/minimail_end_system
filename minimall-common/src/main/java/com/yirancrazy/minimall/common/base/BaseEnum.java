package com.yirancrazy.minimall.common.base;

import com.yirancrazy.minimall.common.result.ResultCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务码枚举接口，继承 ResultCode 并约定 (code, alias, message) 字段契约，默认以枚举常量名作为编码，供各服务错误码枚举实现。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface BaseEnum extends ResultCode {
    @Override
    default String getCode() {
        return ((Enum<?>) this).name();
    }
}