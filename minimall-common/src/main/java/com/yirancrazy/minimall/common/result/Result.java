package com.yirancrazy.minimall.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * 统一接口响应对象，封装响应码、消息、业务数据和链路标识，供所有 Controller 出参使用。
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T>
{
    private String code;
    private String message;
    private T data;
    private String traceId;

    /**
     * 构造无数据的成功响应。
     *
     * @return 响应码为 {@code 00000} 的成功结果，message、data、traceId 均为 null
     */
    public static <T> Result<T> success()
    {
        return new Result<>("00000", null, null, null);
    }

    public static <T> Result<T> success(T data)
    {
        return new Result<>("00000", null, data, null);
    }

    /**
     * 按错误码和提示消息构造失败响应。
     *
     * @param code 业务或系统错误码，遵循业务错误 1xxxx、系统错误 2xxxx 约定
     * @param msg  面向调用方的错误提示消息
     * @return 携带错误码与消息的失败结果，data 为 null
     */
    public static <T> Result<T> fail(String code, String msg)
    {
        return new Result<>(code, msg, null, null);
    }

    public static <T> Result<T> fail(BizException e)
    {
        return new Result<>(e.getCode(), e.getMessage(), null, null);
    }
}