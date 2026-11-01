package com.yirancrazy.minimall.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Manager 层实现的组件标记，同时通过 {@link Service} 注册为 Spring Bean。
 * Manager 负责单一持久化实体的数据访问与简单查询编排，不应承载跨服务业务编排。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface Manager {
    /**
     * 指定注册到 Spring 容器中的 Bean 名称。
     *
     * @return Bean 名称；空字符串表示使用 Spring 默认命名策略
     */
    String value() default "";
}
