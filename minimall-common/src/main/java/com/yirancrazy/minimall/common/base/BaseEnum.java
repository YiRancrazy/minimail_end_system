package com.yirancrazy.minimall.common.base;

import com.yirancrazy.minimall.common.result.ResultCode;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: BaseEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface BaseEnum extends ResultCode {
    /**
     * 获取相关数据。
     * @return 处理结果
     */
    @Override
    default String getCode() {
        return ((Enum<?>) this).name();
    }
}