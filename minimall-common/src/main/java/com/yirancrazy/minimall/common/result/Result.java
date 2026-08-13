package com.yirancrazy.minimall.common.result;

import org.slf4j.MDC;
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
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    /** MDC 中 traceId 的 key，与 logback-spring.xml 的 %X{traceId} 保持一致 */
    public static final String TRACE_ID_KEY = "traceId";

    private String code;
    private String message;
    private T data;
    private String traceId;

    /**
     * 取当前线程 MDC 中的 traceId，未设置时返回 null。
     * @return 链路追踪 ID 或 null
     */
    private static String currentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 构造无数据的成功响应。
     * @param <T> 业务数据类型
     * @return 响应码为 {@code 00000} 的成功结果，message、data 为 null，traceId 取自 MDC
     */
    public static <T> Result<T> success() {
        return new Result<>("00000", null, null, currentTraceId());
    }

    /**
     * 构造携带业务数据的成功响应。
     * @param <T> 业务数据类型
     * @param data 业务数据
     * @return 响应码为 {@code 00000} 的成功结果，message 为 null，traceId 取自 MDC
     */
    public static <T> Result<T> success(T data) {
        return new Result<>("00000", null, data, currentTraceId());
    }

    /**
     * 按错误码和提示消息构造失败响应。
     * @param <T> 业务数据类型
     * @param code 业务或系统错误码，遵循业务错误 1xxxx、系统错误 2xxxx 约定
     * @param msg 面向调用方的错误提示消息
     * @return 携带错误码与消息的失败结果，data 为 null，traceId 取自 MDC
     */
    public static <T> Result<T> fail(String code, String msg) {
        return new Result<>(code, msg, null, currentTraceId());
    }

    /**
     * 从业务异常构造失败响应。
     * @param <T> 业务数据类型
     * @param e 业务异常
     * @return 携带异常错误码与消息的失败结果，data 为 null，traceId 取自 MDC
     */
    public static <T> Result<T> fail(BizException e) {
        return new Result<>(e.getCode(), e.getMessage(), null, currentTraceId());
    }
}