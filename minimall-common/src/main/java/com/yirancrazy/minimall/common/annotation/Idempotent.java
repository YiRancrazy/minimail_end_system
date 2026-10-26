package com.yirancrazy.minimall.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: 接口幂等控制注解
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等键，支持 SpEL 表达式，例如 {@code "#userId + ':' + #orderId"}。
     * 由切面解析后作为唯一标识（通常落到 Redis key）。
     * 为空时回退请求头 X-Idempotency-Key；仍为空则拒绝执行。
     * @return 幂等键的 SpEL 表达式字符串；如果为空则回退请求头
     */
    String key() default "";

    /**
     * 过期时间。
     * @return 过期时间
     */
    long expire() default 24;

    /**
     * 过期时间单位。
     * @return 时间单位
     */
    TimeUnit unit() default TimeUnit.HOURS;
}
