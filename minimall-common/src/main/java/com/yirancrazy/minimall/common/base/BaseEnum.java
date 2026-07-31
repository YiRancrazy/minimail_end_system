package com.yirancrazy.minimall.common.base;

import com.yirancrazy.minimall.common.result.ResultCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务枚举基类接口，定义 code、alias、message 三元组契约，所有业务枚举必须实现。
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface BaseEnum extends ResultCode {
    @Override
    default String getCode() {
        return ((Enum<?>) this).name();
    }
}