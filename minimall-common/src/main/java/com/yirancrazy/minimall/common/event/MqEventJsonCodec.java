package com.yirancrazy.minimall.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON codec for RocketMQ message bodies. Tags use event simpleName;
 * keys use the same convention.
 */
public final class MqEventJsonCodec {
    private static final ObjectMapper M = new ObjectMapper().findAndRegisterModules();

    private MqEventJsonCodec() {}

    /**
     * Serialize the given event object into a JSON byte array suitable for use
     * as a RocketMQ message body. Jackson modules are auto-registered.
     *
     * @param event the domain event to serialize; must not be {@code null}
     * @return the UTF-8 JSON encoding of {@code event}
     * @throws IllegalStateException if Jackson fails to serialize the event
     */
    public static byte[] encode(Object event) {
        try {
            return M.writeValueAsBytes(event);
        } catch (Exception e) {
            throw new IllegalStateException("encode failed: " + event.getClass(), e);
        }
    }

    /**
     * Deserialize the given RocketMQ message body into an instance of the
     * target event type.
     *
     * @param body the raw message bytes received from RocketMQ
     * @param type the target event {@link Class} to decode into
     * @param <T> the target event type
     * @return a populated instance of {@code type}
     * @throws IllegalStateException if Jackson fails to read or bind the body
     */
    public static <T> T decode(byte[] body, Class<T> type) {
        try {
            return M.readValue(body, type);
        } catch (Exception e) {
            throw new IllegalStateException("decode failed: " + type, e);
        }
    }

    /**
     * Derive the RocketMQ routing tag for an event type. The convention is the
     * simple class name, matching the producer side in {@link #encode(Object)}.
     *
     * @param eventType the event {@link Class} to derive the tag for
     * @return the simple class name of {@code eventType}, used as the RocketMQ tag
     */
    public static String tagFor(Class<?> eventType) {
        return eventType.getSimpleName();
    }
}
