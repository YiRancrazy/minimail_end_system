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
public @interface Idempotency {

    /**
     * 指定幂等键表达式。不含 {@code #} 时按字面量使用；以 {@code #} 开头时整体按 SpEL 求值；
     * 其他含 {@code #} 的内容按模板处理，将内嵌变量替换为求值结果，如
     * {@code order:checkout:#userId:#idempotencyKey}。
     * 为空时回退到请求头 X-Idempotency-Key，仍为空则切面拒绝执行。
     *
     * @return 字面量、SpEL 或模板表达式；空字符串表示使用请求头键
     */
    String key() default "";

    /**
     * 指定幂等键的重试类型，用于区分不同重试类型的幂等键。
     *
     * @return 幂等键的重试类型
     */
    String replayType() default "";

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
