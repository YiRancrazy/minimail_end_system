package com.yirancrazy.minimall.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yirancrazy.minimall.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private String code;
    private String message;
    private T data;
    private String traceId;

    public static <T> Result<T> success() {
        return new Result<>("00000", null, null, null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>("00000", null, data, null);
    }

    public static <T> Result<T> fail(String code, String msg) {
        return new Result<>(code, msg, null, null);
    }

    public static <T> Result<T> fail(BizException e) {
        return new Result<>(e.getCode(), e.getMessage(), null, null);
    }
}