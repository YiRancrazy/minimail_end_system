package com.yirancrazy.minimall.common.config;

import com.yirancrazy.minimall.common.aspect.IdempotentAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在 Redis 自动配置完成后注册幂等切面，仅当类路径和容器中存在 StringRedisTemplate 时生效。
 * 该配置使引入 minimall-common 的服务无需重复声明幂等组件。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class IdempotentAutoConfiguration {

    /**
     * 创建并注册使用 Redis 存储幂等键的切面。
     *
     * @param redisTemplate Spring 容器中的字符串 Redis 模板
     * @return 已注入 Redis 依赖的幂等切面
     */
    @Bean
    public IdempotentAspect idempotentAspect(StringRedisTemplate redisTemplate) {
        return new IdempotentAspect(redisTemplate);
    }
}
