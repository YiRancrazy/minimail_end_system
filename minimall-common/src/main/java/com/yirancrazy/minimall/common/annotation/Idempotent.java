package com.yirancrazy.minimall.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: Idempotent 类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    String key();

    long expire() default 24;

    TimeUnit unit() default TimeUnit.HOURS;
}