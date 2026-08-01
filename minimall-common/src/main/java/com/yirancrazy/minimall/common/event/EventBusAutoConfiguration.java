package com.yirancrazy.minimall.common.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线组件，提供事件发布订阅能力
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Configuration
public class EventBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LocalEventBus.class)
    public LocalEventBus localEventBus() {
        return new LocalEventBus();
    }

    /**
     * Expose the in-process {@link LocalEventBus} as the default {@link EventBus}
     * bean when no other implementation is registered in the context.
     *
     * @param local the always-registered {@link LocalEventBus} candidate bean
     * @return the same {@link LocalEventBus} instance, to be used as the fallback {@link EventBus}
     */
    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus defaultEventBus(LocalEventBus local) {
        return local;
    }
}
