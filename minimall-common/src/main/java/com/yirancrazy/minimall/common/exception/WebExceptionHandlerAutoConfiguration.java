package com.yirancrazy.minimall.common.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 注册 {@link GlobalExceptionHandler}，使 Controller 抛出的
 * {@link com.yirancrazy.minimall.common.exception.BizException} 能被处理，
 * 且无论服务从哪个 {@code @SpringBootApplication} 包启动都能加载处理器。
 */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebExceptionHandlerAutoConfiguration {
}
