package com.yirancrazy.minimall.common.annotation;

import com.yirancrazy.minimall.common.constant.PermissionEnum;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 标记需要角色权限校验的方法，由 PermissionAspect 从 X-User-Role 请求头解析角色，
 * 再结合 Redis 权限覆盖配置完成任一或全部权限判断。仅适用于方法级鉴权。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

  /**
   * 指定访问目标方法所需的权限集合，可选择一个或多个权限码。
   *
   * @return 所需权限数组
   */
  PermissionEnum[] value();

  /**
   * 指定权限匹配方式；默认满足任一权限即可，设为 true 时必须拥有全部权限。
   *
   * @return true 表示要求全部权限，false 表示要求任一权限
   */
  boolean requireAll() default false;
}
