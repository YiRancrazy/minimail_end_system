package com.yirancrazy.minimall.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import com.yirancrazy.minimall.common.support.MdcTaskDecorator;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: TraceIdPropagationAutoConfiguration 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@AutoConfiguration
public class TraceIdPropagationAutoConfiguration {

    /**
     * 执行 mdcTaskDecorator 方法。
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }
}
