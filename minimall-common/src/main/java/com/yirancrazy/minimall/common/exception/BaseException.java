package com.yirancrazy.minimall.common.exception;

import lombok.Getter;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: BaseException 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Getter
public class BaseException extends RuntimeException {
    private final String code;

    /**
     * 构造 BaseException 实例。
     * @param code code 参数
     * @param message message 参数
     */
    public BaseException(String code, String message) {
        super(message);
        this.code = code;
    }
}