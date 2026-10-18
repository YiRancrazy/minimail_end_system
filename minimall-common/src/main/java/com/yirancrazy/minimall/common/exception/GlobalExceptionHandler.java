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
 * @Description: GlobalExceptionHandler 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 执行 biz 方法。
     * @param e e 参数
     * @return 统一响应结果
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> biz(BizException e) {
        log.warn("业务异常，code={}，alias={}，msg={}", e.getCode(), e.getAlias(), e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(e.getCode(), e.getMessage()));
    }


    /**
     * 执行 validation 方法。
     * @param e e 参数
     * @return 统一响应结果
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> validation(Exception e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(CommonCode.PARAM_INVALID, "请求参数校验失败"));
    }


    /**
     * 执行 unreadable 方法。
     * @param e e 参数
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> unreadable(HttpMessageNotReadableException e) {
        log.warn("请求体不可读: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(CommonCode.PARAM_INVALID, "请求体格式错误"));
    }


    /**
     * 执行 base 方法。
     * @param e e 参数
     * @return 统一响应结果
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> base(BaseException e) {
        log.error("基础异常，code={}，msg={}", e.getCode(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(e.getCode(), e.getMessage()));
    }


    /**
     * 执行 missingHeader 方法。
     * @param e e 参数
     * @return 统一响应结果
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Result<Void>> missingHeader(MissingRequestHeaderException e) {
        log.warn("缺少必填请求头: {}", e.getHeaderName());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail("14003", "缺少请求头: " + e.getHeaderName()));
    }


    /**
     * 执行 unknown 方法。
     * @param e e 参数
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unknown(Exception e) {
        // 深挖 cause 链，找回被 Seata 代理吞掉类型的 BizException/BaseException，恢复业务错误码语义，避免越权/不存在订单误报"系统繁忙"
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof BizException biz) {
                log.warn("解包后业务异常，code={}，alias={}，msg={}",
                    biz.getCode(), biz.getAlias(), biz.getMessage());
                return ResponseEntity.status(HttpStatus.OK).body(Result.fail(biz.getCode(), biz.getMessage()));
            }
            if (t instanceof BaseException base) {
                log.warn("解包后基础异常，code={}，msg={}", base.getCode(), base.getMessage());
                return ResponseEntity.status(HttpStatus.OK).body(Result.fail(base.getCode(), base.getMessage()));
            }
        }
        log.error("未知异常", e);
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(CommonCode.SYS_ERROR, "系统繁忙"));
    }
}
