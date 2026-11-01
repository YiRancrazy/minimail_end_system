package com.yirancrazy.minimall.common.base;

import com.yirancrazy.minimall.common.result.ResultCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务枚举的统一基础接口，要求实现类同时提供业务码、别名和展示消息。
 * 默认业务码取枚举常量名，供错误码枚举和状态枚举沿用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface BaseEnum extends ResultCode {
    /**
     * 返回枚举常量名作为默认业务码。
     *
     * @return 当前枚举常量的 name
     */
    @Override
    default String getCode() {
        return ((Enum<?>) this).name();
    }
}
