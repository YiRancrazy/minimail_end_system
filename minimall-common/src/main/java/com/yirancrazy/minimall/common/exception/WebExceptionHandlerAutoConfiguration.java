package com.yirancrazy.minimall.common.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 将 GlobalExceptionHandler 自动导入 Web 服务容器，使统一异常映射在组件扫描范围外也能生效。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebExceptionHandlerAutoConfiguration {}
