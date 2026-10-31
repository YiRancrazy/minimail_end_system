package com.yirancrazy.minimall.common.result;

import com.yirancrazy.minimall.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 统一 HTTP 响应包装，包含业务码、消息、数据和当前链路 traceId。
 * 成功与失败工厂方法会自动读取 MDC，Controller 与服务不应手工构造响应字段。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

  public static final String TRACE_ID_KEY = "traceId";

  private String code;
  private String message;
  private T data;
  private String traceId;

  /**
   * 获取当前线程 MDC 中的 traceId，未设置时返回 null.
   *
   * @return 链路追踪 ID 或 null
   */
  private static String currentTraceId() {
    return MDC.get(TRACE_ID_KEY);
  }

  /**
   * 创建不含数据的成功响应，业务码固定为 {@link CommonCode#SUCCESS}。
   *
   * @param <T> 数据类型
   * @return data 为 null 的成功响应
   */
  public static <T> Result<T> success() {
    return new Result<>("00000", null, null, currentTraceId());
  }

  /**
   * 创建携带业务数据的成功响应。
   *
   * @param <T> 数据类型
   * @param data 返回给调用方的业务数据
   * @return 含数据、消息为 null 且带 traceId 的成功响应
   */
  public static <T> Result<T> success(T data) {
    return new Result<>("00000", null, data, currentTraceId());
  }

  /**
   * 使用指定业务码和消息创建失败响应，data 固定为 null。
   *
   * @param <T> 数据类型
   * @param code 业务错误码
   * @param msg 面向调用方的错误消息
   * @return 失败响应
   */
  public static <T> Result<T> fail(String code, String msg) {
    return new Result<>(code, msg, null, currentTraceId());
  }

  /**
   * 从业务异常提取错误码和消息创建失败响应。
   *
   * @param <T> 数据类型
   * @param e 业务异常
   * @return 使用异常码和消息构造的失败响应
   */
  public static <T> Result<T> fail(BizException e) {
    return new Result<>(e.getCode(), e.getMessage(), null, currentTraceId());
  }
}
