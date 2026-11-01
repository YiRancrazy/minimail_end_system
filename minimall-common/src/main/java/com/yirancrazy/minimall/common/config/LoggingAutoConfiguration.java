package com.yirancrazy.minimall.common.config;

import com.yirancrazy.minimall.common.aspect.LoggingAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 自动注册 service 与 controller 通用日志切面，使依赖 minimall-common 的 Web 服务获得统一调用日志。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@AutoConfiguration
public class LoggingAutoConfiguration {

    /**
     * 创建并注册统一日志切面实例。
     *
     * @return 新的 LoggingAspect Bean
     */
    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
