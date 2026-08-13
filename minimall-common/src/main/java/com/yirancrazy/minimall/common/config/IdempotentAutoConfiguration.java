package com.yirancrazy.minimall.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.yirancrazy.minimall.common.aspect.IdempotentAspect;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 幂等切面自动装配，仅在服务具备 Redis（StringRedisTemplate）时注册，
 *               无 Redis 的服务（如部分单测/直连场景）自动跳过，不影响启动。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class IdempotentAutoConfiguration {

    @Bean
    public IdempotentAspect idempotentAspect(StringRedisTemplate redisTemplate) {
        return new IdempotentAspect(redisTemplate);
    }
}
