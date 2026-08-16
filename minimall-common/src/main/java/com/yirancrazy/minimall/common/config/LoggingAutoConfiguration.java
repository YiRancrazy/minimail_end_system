package com.yirancrazy.minimall.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import com.yirancrazy.minimall.common.aspect.LoggingAspect;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 日志自动配置类
 * @Version: 1.0
 * @DateTime: 2026/8/16 9:30
 **/

@AutoConfiguration
public class LoggingAutoConfiguration {

    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
