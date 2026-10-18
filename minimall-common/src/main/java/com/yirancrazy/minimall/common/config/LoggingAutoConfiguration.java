package com.yirancrazy.minimall.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import com.yirancrazy.minimall.common.aspect.LoggingAspect;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: LoggingAutoConfiguration 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@AutoConfiguration
public class LoggingAutoConfiguration {

    /**
     * 执行 loggingAspect 方法。
     * @return 处理结果
     */
    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
