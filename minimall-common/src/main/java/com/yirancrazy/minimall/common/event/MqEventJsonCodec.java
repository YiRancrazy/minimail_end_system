package com.yirancrazy.minimall.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件消息 JSON 编解码工具，统一使用注册 Java 时间模块的 ObjectMapper，
 * 并以事件类简单名作为 RocketMQ tag。编码或解码失败会转换为 IllegalStateException。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public final class MqEventJsonCodec {
  private static final ObjectMapper M = new ObjectMapper().findAndRegisterModules();

  /**   禁止实例化工具类. */
  private MqEventJsonCodec() {}

  /**
   * 将事件对象编码为 JSON 字节数组.
   *
   * @param event 待编码的事件对象
   * @return JSON 字节数组
   * @throws IllegalStateException 事件编码失败时抛出
   */
  public static byte[] encode(Object event) {
    try {
      return M.writeValueAsBytes(event);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("事件编码失败：" + event.getClass(), e);
    }
  }

  /**
   * 将 JSON 字节数组解码为指定类型的事件对象.
   *
   * @param <T> 事件类型
   * @param body JSON 字节数组
   * @param type 目标事件类型
   * @return 解码后的事件对象
   * @throws IllegalStateException 事件解码失败时抛出
   */
  public static <T> T decode(byte[] body, Class<T> type) {
    try {
      return M.readValue(body, type);
    } catch (IOException e) {
      throw new IllegalStateException("事件解码失败：" + type, e);
    }
  }

  /**
   * 获取事件类型对应的 RocketMQ tag.
   *
   * @param eventType 事件类型
   * @return 事件类简单名
   */
  public static String tagFor(Class<?> eventType) {
    return eventType.getSimpleName();
  }
}
