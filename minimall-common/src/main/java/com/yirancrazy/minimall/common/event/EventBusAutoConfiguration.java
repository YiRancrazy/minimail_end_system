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
 * @Description: 事件总线自动配置，按条件注册本地或 RocketMQ 事件总线实现。
 *               默认注册本地实现；开启 minimall.eventbus.rocketmq.enabled 后额外注册
 *               RocketMQ 实现并作为首选注入。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Configuration
public class EventBusAutoConfiguration {

    /**
     * 注册本地事件总线。
     * 仅在容器中不存在 LocalEventBus 类型的 Bean 时生效。
     *
     * @return 本地事件总线实例
     */
    @Bean
    @ConditionalOnMissingBean(LocalEventBus.class)
    public LocalEventBus localEventBus() {
        return new LocalEventBus();
    }


    /**
     * 注册默认事件总线。
     * 仅在容器中不存在 EventBus 类型的 Bean 时生效。
     *
     * @param local 本地事件总线实例
     * @return 作为默认实现的事件总线实例
     */
    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus defaultEventBus(LocalEventBus local) {
        return local;
    }


    /**
     * 注册 RocketMQ 事件总线。
     * 仅在 minimall.eventbus.rocketmq.enabled=true 时生效，并作为 EventBus 的首选实现。
     *
     * @param namesrvAddr RocketMQ NameServer 地址，默认 127.0.0.1:9876
     * @param topic 事件消息主题，默认 minimall-events
     * @param outboxProvider 本地消息表存储提供者；无可用实例时不设置
     * @return 已完成基础配置的 RocketMQ 事件总线实例
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
            // 配置后可由本地消息表在 broker 回查时恢复事务状态。
            bus.setOutboxStore(store);
        }
        return bus;
    }
}
