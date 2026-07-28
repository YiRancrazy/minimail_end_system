package com.yirancrazy.minimall.common.base;

import com.yirancrazy.minimall.common.result.ResultCode;

public interface BaseEnum extends ResultCode {
    @Override
    default String getCode() {
        return ((Enum<?>) this).name();
    }
}