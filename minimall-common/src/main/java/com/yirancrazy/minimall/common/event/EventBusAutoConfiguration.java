package com.yirancrazy.minimall.common.event;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: EventBusAutoConfiguration 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Configuration
public class EventBusAutoConfiguration {

    /**
     * 执行 localEventBus 方法。
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(LocalEventBus.class)
    public LocalEventBus localEventBus() {
        return new LocalEventBus();
    }


    /**
     * 执行 defaultEventBus 方法。
     * @param local local 参数
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus defaultEventBus(LocalEventBus local) {
        return local;
    }


    /**
     * 执行 rocketMqEventBus 方法。
     * @param namesrvAddr namesrvAddr 参数
     * @param topic topic 参数
     * @param outboxProvider outboxProvider 参数
     * @return 处理结果
     */
    @Bean
    @ConditionalOnProperty(name = "minimall.eventbus.rocketmq.enabled", havingValue = "true")
    @Primary
    public RocketMqEventBus rocketMqEventBus(
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic,
        ObjectProvider<OutboxStore> outboxProvider) {
        RocketMqEventBus bus = new RocketMqEventBus(namesrvAddr, topic);
        OutboxStore store = outboxProvider.getIfAvailable();
        if (store != null) {
            bus.setOutboxStore(store);
        }
        return bus;
    }
}
