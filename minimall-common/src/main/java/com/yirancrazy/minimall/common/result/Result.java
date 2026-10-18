package com.yirancrazy.minimall.common.result;

import org.slf4j.MDC;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.yirancrazy.minimall.common.exception.BizException;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: Result 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

    public static final String TRACE_ID_KEY = "traceId";

    private String code;
    private String message;
    private T data;
    private String traceId;


    /**
     * 获取当前线程 MDC 中的 traceId，未设置时返回 null。
     * @return 链路追踪 ID 或 null
     */
    private static String currentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }


    /**
     * 执行 success 方法。
     * @param <T> 泛型参数
     * @return 统一响应结果
     */
    public static <T> Result<T> success() {
        return new Result<>("00000", null, null, currentTraceId());
    }


    /**
     * 执行 success 方法。
     * @param <T> 泛型参数
     * @param data data 参数
     * @return 统一响应结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>("00000", null, data, currentTraceId());
    }


    /**
     * 执行 fail 方法。
     * @param <T> 泛型参数
     * @param code code 参数
     * @param msg msg 参数
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(String code, String msg) {
        return new Result<>(code, msg, null, currentTraceId());
    }


    /**
     * 执行 fail 方法。
     * @param <T> 泛型参数
     * @param e e 参数
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(BizException e) {
        return new Result<>(e.getCode(), e.getMessage(), null, currentTraceId());
    }
}
