package com.yirancrazy.minimall.common.config;

import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Jackson配置类，配置Jackson相关 Bean
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
            .modules(new JavaTimeModule())
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializerByType(BigDecimal.class, ToStringSerializer.instance);
    }
}