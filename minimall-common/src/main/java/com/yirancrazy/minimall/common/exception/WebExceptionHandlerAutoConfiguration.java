package com.yirancrazy.minimall.common.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Registers {@link GlobalExceptionHandler} so that controllers throw
 * {@link com.yirancrazy.minimall.common.exception.BizException} and the handler
 * is picked up regardless of which {@code @SpringBootApplication} package the
 * service starts in.
 */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebExceptionHandlerAutoConfiguration {
}