package com.yirancrazy.minimall.common.event;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * RocketMQ producer implementation. Activated by config
 * `minimall.eventbus.rocketmq.enabled=true` (default false). Falls back to
 * WARN-on-failure so producer outages never block the publish site.
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(prefix = "minimall.eventbus.rocketmq", name = "enabled", havingValue = "true")
public class RocketMqEventBus implements EventBus
{

    private final String namesrvAddr;
    private final String topic;
    private DefaultMQProducer producer;

    public RocketMqEventBus(
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic)
    {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
    }

    @PostConstruct
    void start()
    {
        producer = new DefaultMQProducer("minimall-producer");
        producer.setNamesrvAddr(namesrvAddr);
        try
        {
            producer.start();
            log.info("rocketmq producer started, namesrv={}, topic={}", namesrvAddr, topic);
        }
        catch (MQClientException e)
        {
            log.warn("rocketmq producer start failed, publish will be no-op: {}", e.getMessage());
            producer = null;
        }
    }

    @PreDestroy
    void stop()
    {
        if (producer != null)
        {
            producer.shutdown();
        }
    }

    /**
     * Deliver the given event to the configured RocketMQ topic using the
     * event's simple class name as the routing tag and its JSON encoding as
     * the message body. Producer outages are logged at WARN and swallowed so
     * the publish site is never blocked.
     *
     * @param event the domain event to send; encoded via {@link MqEventJsonCodec}
     */
    @Override
    public void publish(Object event)
    {
        if (producer == null)
        {
            log.warn("rocketmq producer not started; event {} dropped", event.getClass().getSimpleName());
            return;
        }
        try
        {
            Message msg = new Message(topic, MqEventJsonCodec.tagFor(event.getClass()),
                MqEventJsonCodec.encode(event));
            SendResult r = producer.send(msg);
            log.debug("rocketmq send ok, msgId={}", r.getMsgId());
        }
        catch (MQClientException | RemotingException e)
        {
            log.warn("rocketmq send failed for {}: {}", event.getClass().getSimpleName(), e.getMessage());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            log.warn("rocketmq send interrupted for {}", event.getClass().getSimpleName());
        }
    }
}
