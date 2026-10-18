package com.yirancrazy.minimall.common.result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ResultCode 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface ResultCode {
    /**
     * 获取相关数据。
     * @return 处理结果
     */
    String getCode();

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    String getAlias();

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    String getMessage();
}
