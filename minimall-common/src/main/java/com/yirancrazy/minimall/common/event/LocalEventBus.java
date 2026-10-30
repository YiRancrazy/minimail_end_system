package com.yirancrazy.minimall.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 进程内事件总线实现，封装 Spring ApplicationEventPublisher。
 *               业务代码仅依赖 EventBus 接口，不直接依赖 Spring 事件 API。
 *               本类未标注 @Component，由 EventBusAutoConfiguration 通过 @Bean 注册。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class LocalEventBus implements ApplicationEventPublisherAware, EventBus {

    private ApplicationEventPublisher publisher;


    /**
     * 注入 Spring 事件发布器。
     * 该方法由 Spring 在 Bean 初始化阶段回调，业务代码不应手动调用。
     *
     * @param publisher Spring 事件发布器
     */
    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * 发布事件到 Spring 事件广播机制。
     * 普通对象会由 Spring 包装为 PayloadApplicationEvent 后分发；监听器默认在调用线程中同步执行。
     *
     * @param event 待发布的事件对象
     * @throws IllegalStateException 事件发布器尚未注入时抛出
     */
    @Override
    public void publish(Object event) {
        if (publisher == null) {
            throw new IllegalStateException("LocalEventBus 未初始化");
        }
        publisher.publishEvent(event);
    }


    /**
     * 使用显式路由发布本地事件。
     * 本地事件总线不连接 MQ，因此路由仅做参数校验，不参与事件分发。
     *
     * @param topic MQ 主题
     * @param tag MQ 标签
     * @param event 待发布的事件对象
     */
    @Override
    public void publish(String topic, String tag, Object event) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic 不能为空");
        }
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag 不能为空");
        }
        publish(event);
    }
}
