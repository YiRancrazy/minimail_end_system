package com.yirancrazy.minimall.common.exception;

import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 系统异常基类，继承 RuntimeException 并额外承载错误码字段，供框架层与基础设施层异常继承，由全局异常处理器统一转换为失败响应。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Getter
public class BaseException extends RuntimeException
{
    private final String code;

    public BaseException(String code, String message)
    {
        super(message);
        this.code = code;
    }
}