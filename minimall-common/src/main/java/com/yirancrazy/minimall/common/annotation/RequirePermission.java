package com.yirancrazy.minimall.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.yirancrazy.minimall.common.constant.PermissionEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: RequirePermission 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {


    /**
     * 执行 value 方法。
     * @return 结果列表
     */
    PermissionEnum[] value();


    /**
     * 执行 requireAll 方法。
     * @return 是否处理成功
     */
    boolean requireAll() default false;
}
