package com.yirancrazy.minimall.common.exception;

import com.yirancrazy.minimall.common.result.ResultCode;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务失败的标准运行时异常，承载业务码、可选英文别名和用户可读消息。
 * GlobalExceptionHandler 会将其转换为 HTTP 200 的统一失败响应，并记录错误别名。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
public class BizException extends RuntimeException {
    private final String code;
    private final String alias;

    /**
     * 创建只包含业务码和消息的业务异常，别名保持为空。
     *
     * @param code 业务错误码
     * @param message 面向调用方的错误消息
     */
    public BizException(String code, String message) {
        super(message);
        this.code = code;
        this.alias = null;
    }

    /**
     * 创建包含业务码、别名和消息的业务异常。
     *
     * @param code 业务错误码
     * @param alias 用于日志检索的稳定英文别名
     * @param message 面向调用方的错误消息
     */
    public BizException(String code, String alias, String message) {
        super(message);
        this.code = code;
        this.alias = alias;
    }

    /**
     * 创建保留原始原因的业务异常，用于解密失败、远程调用失败等需要诊断链路的场景。
     *
     * @param code 业务错误码
     * @param alias 用于日志检索的稳定英文别名
     * @param message 面向调用方的错误消息
     * @param cause 触发业务失败的原始异常
     */
    public BizException(String code, String alias, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.alias = alias;
    }

    /**
     * 从统一结果码枚举创建业务异常。
     *
     * @param rc 提供业务码、别名和消息的结果码
     */
    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
        this.alias = rc.getAlias();
    }
}
