package com.yirancrazy.minimall.common.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.yirancrazy.minimall.common.aspect.PermissionAspect;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 权限校验切面自动装配，注册 PermissionAspect 使其在业务服务中生效。
 * @Version: 1.0
 * @DateTime: 2026/8/16 9:31
 **/

@AutoConfiguration
public class PermissionAutoConfiguration {

    @Bean
    public PermissionAspect permissionAspect(ObjectProvider<StringRedisTemplate> redisProvider) {
        return new PermissionAspect(redisProvider.getIfAvailable());
    }
}
