package com.yirancrazy.minimall.common.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: WebExceptionHandlerAutoConfiguration 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebExceptionHandlerAutoConfiguration {
}
