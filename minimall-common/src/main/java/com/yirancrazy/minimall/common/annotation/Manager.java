package com.yirancrazy.minimall.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Service;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 数据访问层，继承 IService，提供基础 CRUD 操作。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface Manager {
    String value() default "";
}