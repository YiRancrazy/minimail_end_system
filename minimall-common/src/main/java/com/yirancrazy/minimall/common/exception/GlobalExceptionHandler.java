package com.yirancrazy.minimall.common.exception;

import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> biz(BizException e) {
        log.warn("biz exception code={} alias={} msg={}", e.getCode(), e.getAlias(), e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> validation(Exception e) {
        log.warn("validation failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(CommonCode.PARAM_INVALID, "请求参数校验失败"));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> base(BaseException e) {
        log.error("base error code={} msg={}", e.getCode(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Result<Void>> unknown(Throwable e) {
        log.error("unknown error", e);
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(CommonCode.SYS_ERROR, "系统繁忙"));
    }
}