package com.yirancrazy.minimall.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 标记需要幂等保护的写接口，由 IdempotentAspect 根据 SpEL 或请求头解析幂等键，
 * 并通过 Redis 抢占键阻止重复请求。仅作用于方法，用法与过期策略必须与具体写操作匹配。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

  /**
   * 指定幂等键表达式。非空且以 {@code #} 开头时按 SpEL 从方法参数求值，否则按字面量使用；
   * 为空时回退到请求头 X-Idempotency-Key，仍为空则切面拒绝执行。
   *
   * @return SpEL 表达式或字面量键；空字符串表示使用请求头键
   */
  String key() default "";

  /**
   * 指定幂等键在 Redis 中的保留时长，时长耗尽前相同幂等键无法再次执行目标方法。
   *
   * @return 保留时长数值，单位由 {@link #unit()} 决定
   */
  long expire() default 24;

  /**
   * 指定 {@link #expire()} 的时间单位。
   *
   * @return 幂等键过期时间单位
   */
  TimeUnit unit() default TimeUnit.HOURS;
}
