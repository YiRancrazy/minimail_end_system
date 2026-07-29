package com.yirancrazy.minimall.common.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Decides which {@link EventBus} bean to expose. LocalEventBus is always
 * registered when no other implementation is found; RocketMqEventBus is
 * exposed only when its @ConditionalOnProperty triggers.
 */
@Configuration
public class EventBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus defaultEventBus(LocalEventBus local) {
        return local;
    }
}
