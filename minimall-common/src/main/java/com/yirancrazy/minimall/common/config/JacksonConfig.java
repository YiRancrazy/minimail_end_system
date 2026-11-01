package com.yirancrazy.minimall.common.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 统一 HTTP JSON 序列化规则，启用 Java 时间模块并关闭时间戳输出，
 * 同时把 Long、long 和 BigDecimal 序列化为字符串，避免前端精度丢失。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Configuration
public class JacksonConfig {

    /**
     * 注册 JavaTimeModule、ISO 时间输出以及大整数和高精度小数的字符串序列化器。
     *
     * @return 由 Spring Boot Jackson 自动配置应用的定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder ->
                builder
                        .modules(new JavaTimeModule())
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .serializerByType(Long.class, ToStringSerializer.instance)
                        .serializerByType(Long.TYPE, ToStringSerializer.instance)
                        .serializerByType(BigDecimal.class, ToStringSerializer.instance);
    }
}
