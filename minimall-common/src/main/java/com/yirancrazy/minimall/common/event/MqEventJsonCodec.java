package com.yirancrazy.minimall.common.event;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RocketMQ 消息体的 JSON 编解码器。Tag 使用事件 simpleName；
 * key 使用相同约定。
 */
public final class MqEventJsonCodec {
    private static final ObjectMapper M = new ObjectMapper().findAndRegisterModules();

    private MqEventJsonCodec() {
    }

    /**
     * 将给定事件对象序列化为可用作 RocketMQ 消息体的 JSON 字节数组。
     * Jackson 模块自动注册。
     *
     * @param event 要序列化的领域事件；不能为 {@code null}
     * @return {@code event} 的 UTF-8 JSON 编码
     * @throws IllegalStateException Jackson 序列化事件失败时抛出
     */
    public static byte[] encode(Object event) {
        try {
            return M.writeValueAsBytes(event);
        }
        catch (JsonProcessingException e) {
            throw new IllegalStateException("事件编码失败：" + event.getClass(), e);
        }
    }

    /**
     * 将给定 RocketMQ 消息体反序列化为目标事件类型实例。
     *
     * @param body 从 RocketMQ 接收到的原始消息字节
     * @param type 要解码到的目标事件 {@link Class}
     * @param <T> 目标事件类型
     * @return 已填充的 {@code type} 实例
     * @throws IllegalStateException Jackson 读取或绑定消息体失败时抛出
     */
    public static <T> T decode(byte[] body, Class<T> type) {
        try {
            return M.readValue(body, type);
        }
        catch (IOException e) {
            throw new IllegalStateException("事件解码失败：" + type, e);
        }
    }

    /**
     * 为事件类型推导 RocketMQ 路由 Tag。约定使用简单类名，
     * 与生产者侧 {@link #encode(Object)} 保持一致。
     *
     * @param eventType 要推导 Tag 的事件 {@link Class}
     * @return {@code eventType} 的简单类名，用作 RocketMQ Tag
     */
    public static String tagFor(Class<?> eventType) {
        return eventType.getSimpleName();
    }
}
