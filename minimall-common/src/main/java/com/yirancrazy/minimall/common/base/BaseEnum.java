package com.yirancrazy.minimall.common.base;

import com.yirancrazy.minimall.common.result.ResultCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: BaseEnum description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface BaseEnum extends ResultCode {
    @Override
    default String getCode() {
        return ((Enum<?>) this).name();
    }
}