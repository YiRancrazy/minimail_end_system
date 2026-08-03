package com.yirancrazy.minimall.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.yirancrazy.minimall.common.constant.PermissionEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 接口权限注解，标注在 Controller 方法上声明所需权限，由 PermissionAspect 拦截校验。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 所需权限列表。
     * @return 权限数组
     */
    PermissionEnum[] value();

    /**
     * 是否要求同时拥有全部权限；默认 false，即满足任意一个即可。
     * @return true 表示需要全部权限
     */
    boolean requireAll() default false;
}
