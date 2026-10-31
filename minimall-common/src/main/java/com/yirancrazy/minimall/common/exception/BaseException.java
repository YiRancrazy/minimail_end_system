package com.yirancrazy.minimall.common.exception;

import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 携带稳定错误码的运行时异常基类，供需要区分业务码但不受 BizException 约束的场景使用。
 * 异常消息会直接进入统一异常处理响应，不应包含堆栈或敏感信息。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
public class BaseException extends RuntimeException {
  private final String code;

  /**
   * 创建带错误码和调用方可见消息的基础异常。
   *
   * @param code 稳定错误码
   * @param message 面向调用方的错误消息
   */
  public BaseException(String code, String message) {
    super(message);
    this.code = code;
  }
}
