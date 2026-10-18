package com.yirancrazy.minimall.common.exception;

import lombok.Getter;
import com.yirancrazy.minimall.common.result.ResultCode;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: BizException 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Getter
public class BizException extends RuntimeException {
    private final String code;
    private final String alias;

    /**
     * 构造 BizException 实例。
     * @param code code 参数
     * @param message message 参数
     */
    public BizException(String code, String message) {
        super(message);
        this.code = code;
        this.alias = null;
    }

    /**
     * 构造 BizException 实例。
     * @param code code 参数
     * @param alias alias 参数
     * @param message message 参数
     */
    public BizException(String code, String alias, String message) {
        super(message);
        this.code = code;
        this.alias = alias;
    }

    /**
     * 构造 BizException 实例。
     * @param code code 参数
     * @param alias alias 参数
     * @param message message 参数
     * @param cause cause 参数
     */
    public BizException(String code, String alias, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.alias = alias;
    }

    /**
     * 构造 BizException 实例。
     * @param rc rc 参数
     */
    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
        this.alias = rc.getAlias();
    }
}