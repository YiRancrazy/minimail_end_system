package com.yirancrazy.minimall.common.event;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ producer implementation. Activated by config
 * `minimall.eventbus.rocketmq.enabled=true` (default false). Falls back to
 * WARN-on-failure so producer outages never block the publish site.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "minimall.eventbus.rocketmq", name = "enabled", havingValue = "true")
public class RocketMqEventBus implements EventBus {

    private final String namesrvAddr;
    private final String topic;
    private DefaultMQProducer producer;

    public RocketMqEventBus(
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic) {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
    }

    @PostConstruct
    void start() {
        producer = new DefaultMQProducer("minimall-producer");
        producer.setNamesrvAddr(namesrvAddr);
        try {
            producer.start();
            log.info("rocketmq producer started, namesrv={}, topic={}", namesrvAddr, topic);
        } catch (Exception e) {
            log.warn("rocketmq producer start failed, publish will be no-op: {}", e.getMessage());
            producer = null;
        }
    }

    @PreDestroy
    void stop() {
        if (producer != null) {
            producer.shutdown();
        }
    }

    @Override
    public void publish(Object event) {
        if (producer == null) {
            log.warn("rocketmq producer not started; event {} dropped", event.getClass().getSimpleName());
            return;
        }
        try {
            Message msg = new Message(topic, MqEventJsonCodec.tagFor(event.getClass()),
                MqEventJsonCodec.encode(event));
            SendResult r = producer.send(msg);
            log.debug("rocketmq send ok, msgId={}", r.getMsgId());
        } catch (Exception e) {
            log.warn("rocketmq send failed for {}: {}", event.getClass().getSimpleName(), e.getMessage());
        }
    }
}
