package com.yirancrazy.minimall.common.exception;

import lombok.Getter;
import com.yirancrazy.minimall.common.result.ResultCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Biz异常类，表示Biz相关业务异常
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Getter
public class BizException extends RuntimeException {
    private final String code;
    private final String alias;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
        this.alias = null;
    }

    public BizException(String code, String alias, String message) {
        super(message);
        this.code = code;
        this.alias = alias;
    }

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
        this.alias = rc.getAlias();
    }
}