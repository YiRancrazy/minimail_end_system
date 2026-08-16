package com.yirancrazy.minimall.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: GlobalException处理器，处理GlobalException相关逻辑
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> biz(BizException e) {
        log.warn("biz exception code={} alias={} msg={}", e.getCode(), e.getAlias(), e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 处理请求参数校验失败异常，返回参数非法的统一响应。
     *
     * @param e 由 @Valid 校验触发的 MethodArgumentNotValidException 或 BindException
     * @return HTTP 200 包装的失败响应，错误码为 {@link CommonCode#PARAM_INVALID}
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> validation(Exception e) {
        log.warn("validation failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(CommonCode.PARAM_INVALID, "请求参数校验失败"));
    }

    /**
     * 处理 JSON 反序列化失败（字段类型不匹配、非法枚举值等），避免落入 unknown 返回模糊的"系统繁忙"。
     *
     * @param e 请求体不可读异常
     * @return HTTP 200 包装的失败响应，错误码为 {@link CommonCode#PARAM_INVALID}
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> unreadable(HttpMessageNotReadableException e) {
        log.warn("request body not readable: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(CommonCode.PARAM_INVALID, "请求体格式错误"));
    }

    /**
     * 处理框架层基础异常，记录完整堆栈并返回其自带错误码。
     *
     * @param e 携带错误码与消息的基础异常
     * @return HTTP 200 包装的失败响应，错误码取自异常本身
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> base(BaseException e) {
        log.error("base error code={} msg={}", e.getCode(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 处理必填请求头缺失异常，提示调用方补齐对应请求头。
     *
     * @param e 缺失请求头异常，可从中读取缺失的请求头名称
     * @return HTTP 200 包装的失败响应，错误码为 {@code 14003}，消息含缺失的请求头名
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Result<Void>> missingHeader(MissingRequestHeaderException e) {
        log.warn("missing required header: {}", e.getHeaderName());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail("14003", "缺少请求头: " + e.getHeaderName()));
    }

    /**
     * 兜底处理未被其他处理器捕获的异常，记录完整堆栈并返回系统繁忙提示。
     * 仅捕获 Exception，Error（OOM、StackOverflow 等）交由容器处理，避免掩盖 JVM 致命错误。
     *
     * @param e 任意未预期的异常
     * @return HTTP 200 包装的失败响应，错误码为 {@link CommonCode#SYS_ERROR}，不对外暴露内部细节
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unknown(Exception e) {
        log.error("unknown error", e);
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(CommonCode.SYS_ERROR, "系统繁忙"));
    }
}