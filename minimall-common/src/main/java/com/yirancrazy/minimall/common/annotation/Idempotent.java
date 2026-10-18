package com.yirancrazy.minimall.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: Idempotent 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 执行 key 方法。
     * @return 处理结果
     */
    String key() default "";

    /**
     * 执行 expire 方法。
     * @return 业务数值
     */
    long expire() default 24;

    /**
     * 执行 unit 方法。
     * @return 处理结果
     */
    TimeUnit unit() default TimeUnit.HOURS;
}
