package com.yirancrazy.minimall.common.event;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MqEventJsonCodec 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public final class MqEventJsonCodec {
    private static final ObjectMapper M = new ObjectMapper().findAndRegisterModules();

    /**
     * 构造 MqEventJsonCodec 实例。
     */
    private MqEventJsonCodec() {
    }


    /**
     * 执行 encode 方法。
     * @param event event 参数
     * @return 结果列表
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
     * 执行 decode 方法。
     * @param <T> 泛型参数
     * @param body body 参数
     * @param type type 参数
     * @return 处理结果
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
     * 执行 tagFor 方法。
     * @param eventType eventType 参数
     * @return 处理结果
     */
    public static String tagFor(Class<?> eventType) {
        return eventType.getSimpleName();
    }
}
