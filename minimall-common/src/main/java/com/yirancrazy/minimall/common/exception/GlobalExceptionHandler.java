package com.yirancrazy.minimall.common.exception;

import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Web 层统一异常转换器，将业务异常、参数校验、请求体解析、基础异常和未知异常
 * 转换为 Result 响应。未知异常只向调用方暴露通用消息，详细堆栈仅写入服务日志。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 将业务异常转换为 HTTP 200 失败响应，保留异常中的业务码和消息。
     *
     * @param e 业务异常
     * @return 使用异常业务码构造的失败响应
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> biz(BizException e) {
        log.warn("业务异常，code={}，alias={}，msg={}", e.getCode(), e.getAlias(), e.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 将请求体或参数绑定校验失败转换为统一参数错误，不回传字段级内部细节。
     *
     * @param e 参数校验异常
     * @return 参数错误码和通用校验消息
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> validation(Exception e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK)
                .body(Result.fail(CommonCode.PARAM_INVALID, "请求参数校验失败"));
    }

    /**
     * 处理无法解析的 JSON 请求体，返回通用参数错误而不暴露解析器细节。
     *
     * @param e 请求体不可读异常
     * @return 参数错误码和请求体格式错误消息
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> unreadable(HttpMessageNotReadableException e) {
        log.warn("请求体不可读: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK)
                .body(Result.fail(CommonCode.PARAM_INVALID, "请求体格式错误"));
    }

    /**
     * 将基础异常转换为响应，同时以错误级别记录完整堆栈。
     *
     * @param e 基础异常
     * @return 使用基础异常码构造的失败响应
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Void>> base(BaseException e) {
        log.error("基础异常，code={}，msg={}", e.getCode(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 处理缺少必需请求头的异常，响应中包含缺失的请求头名称。
     *
     * @param e 缺失请求头异常
     * @return 固定的缺少请求头错误码和具体请求头消息
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Result<Void>> missingHeader(MissingRequestHeaderException e) {
        log.warn("缺少必填请求头: {}", e.getHeaderName());
        return ResponseEntity.status(HttpStatus.OK)
                .body(Result.fail("14003", "缺少请求头: " + e.getHeaderName()));
    }

    /**
     * 处理未分类异常。转换前会沿 cause 链查找被代理包装的业务异常，
     * 以保留真实业务错误码；仍未命中时返回系统繁忙并记录完整堆栈。
     *
     * @param e 未分类异常
     * @return 解包后的业务失败响应或通用系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unknown(Exception e) {
        // 深挖 cause 链，找回被 Seata 代理吞掉类型的 BizException/BaseException，恢复业务错误码语义，避免越权/不存在订单误报"系统繁忙"
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof BizException biz) {
                log.warn(
                        "解包后业务异常，code={}，alias={}，msg={}", biz.getCode(), biz.getAlias(), biz.getMessage());
                return ResponseEntity.status(HttpStatus.OK)
                        .body(Result.fail(biz.getCode(), biz.getMessage()));
            }
            if (t instanceof BaseException base) {
                log.warn("解包后基础异常，code={}，msg={}", base.getCode(), base.getMessage());
                return ResponseEntity.status(HttpStatus.OK)
                        .body(Result.fail(base.getCode(), base.getMessage()));
            }
        }
        log.error("未知异常", e);
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(CommonCode.SYS_ERROR, "系统繁忙"));
    }
}
