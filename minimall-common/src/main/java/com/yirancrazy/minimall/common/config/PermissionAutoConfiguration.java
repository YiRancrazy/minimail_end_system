package com.yirancrazy.minimall.common.config;

import com.yirancrazy.minimall.common.aspect.PermissionAspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 自动注册方法级权限切面，并通过 ObjectProvider 容忍 Redis 模板暂时缺失；
 * 缺失时切面采用拒绝访问策略，而不是跳过鉴权。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@AutoConfiguration
public class PermissionAutoConfiguration {

  /**
   * 创建权限切面，并注入可用的 Redis 权限模板。
   *
   * @param redisProvider Redis 模板提供者，容器无对应 Bean 时返回空
   * @return 使用可用 Redis 模板的权限切面
   */
  @Bean
  public PermissionAspect permissionAspect(ObjectProvider<StringRedisTemplate> redisProvider) {
    return new PermissionAspect(redisProvider.getIfAvailable());
  }
}
