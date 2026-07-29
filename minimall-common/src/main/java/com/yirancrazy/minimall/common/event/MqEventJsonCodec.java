package com.yirancrazy.minimall.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON codec for RocketMQ message bodies. Tags use event simpleName;
 * keys use the same convention.
 */
public final class MqEventJsonCodec {
    private static final ObjectMapper M = new ObjectMapper().findAndRegisterModules();

    private MqEventJsonCodec() {}

    public static byte[] encode(Object event) {
        try {
            return M.writeValueAsBytes(event);
        } catch (Exception e) {
            throw new IllegalStateException("encode failed: " + event.getClass(), e);
        }
    }

    public static <T> T decode(byte[] body, Class<T> type) {
        try {
            return M.readValue(body, type);
        } catch (Exception e) {
            throw new IllegalStateException("decode failed: " + type, e);
        }
    }

    public static String tagFor(Class<?> eventType) {
        return eventType.getSimpleName();
    }
}
