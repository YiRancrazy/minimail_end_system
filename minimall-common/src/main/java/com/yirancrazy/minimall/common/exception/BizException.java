package com.yirancrazy.minimall.common.exception;

import com.yirancrazy.minimall.common.result.ResultCode;
import lombok.Getter;

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