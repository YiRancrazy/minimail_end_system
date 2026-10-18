package com.yirancrazy.minimall.common.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.yirancrazy.minimall.common.aspect.PermissionAspect;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PermissionAutoConfiguration 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@AutoConfiguration
public class PermissionAutoConfiguration {

    /**
     * 执行 permissionAspect 方法。
     * @param redisProvider redisProvider 参数
     * @return 处理结果
     */
    @Bean
    public PermissionAspect permissionAspect(ObjectProvider<StringRedisTemplate> redisProvider) {
        return new PermissionAspect(redisProvider.getIfAvailable());
    }
}
