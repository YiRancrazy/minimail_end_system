package com.yirancrazy.minimall.common.exception;

import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Base异常类，表示Base相关业务异常
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Getter
public class BaseException extends RuntimeException {
    private final String code;

    public BaseException(String code, String message) {
        super(message);
        this.code = code;
    }
}