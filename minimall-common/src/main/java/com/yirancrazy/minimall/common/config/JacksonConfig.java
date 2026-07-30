package com.yirancrazy.minimall.common.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
* Jackson 序列化配置，注册 JavaTimeModule 以支持 Java 8 时间类型，并关闭日期时间戳序列化，统一各服务的 JSON 出参格式。
 */
@Configuration
public class JacksonConfig
{

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer()
    {
        return builder -> builder
            .modules(new JavaTimeModule())
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}