package com.yirancrazy.minimall.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Result，提供公共相关能力
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class Result<T> {
    private String code;
    private String message;
    private T data;
    private String traceId;

    /**
     * 构造无数据的成功响应。
     * @param <T> 业务数据类型
     * @return 响应码为 {@code 00000} 的成功结果，message、data、traceId 均为 null
     */
    public static <T> Result<T> success() {
        return new Result<>("00000", null, null, null);
    }

    /**
     * 构造携带业务数据的成功响应。
     * @param <T> 业务数据类型
     * @param data 业务数据
     * @return 响应码为 {@code 00000} 的成功结果，message 和 traceId 为 null
     */
    public static <T> Result<T> success(T data) {
        return new Result<>("00000", null, data, null);
    }

    /**
     * 按错误码和提示消息构造失败响应。
     * @param <T> 业务数据类型
     * @param code 业务或系统错误码，遵循业务错误 1xxxx、系统错误 2xxxx 约定
     * @param msg 面向调用方的错误提示消息
     * @return 携带错误码与消息的失败结果，data 为 null
     */
    public static <T> Result<T> fail(String code, String msg) {
        return new Result<>(code, msg, null, null);
    }

    /**
     * 从业务异常构造失败响应。
     * @param <T> 业务数据类型
     * @param e 业务异常
     * @return 携带异常错误码与消息的失败结果，data 为 null
     */
    public static <T> Result<T> fail(BizException e) {
        return new Result<>(e.getCode(), e.getMessage(), null, null);
    }
}