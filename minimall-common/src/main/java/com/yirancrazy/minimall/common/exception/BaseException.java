package com.yirancrazy.minimall.common.exception;

import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: BaseException description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class BaseException extends RuntimeException {
    private final String code;

    public BaseException(String code, String message) {
        super(message);
        this.code = code;
    }
}