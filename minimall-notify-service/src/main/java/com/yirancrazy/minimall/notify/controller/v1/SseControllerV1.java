package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.notify.sse.SseHub;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供通知 SSE 订阅入口，以 text/event-stream 建立长连接，并交由 SseHub 完成身份一致性和连接配额校验.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/notify/sse")
@RequiredArgsConstructor
public class SseControllerV1 {

  private final SseHub sseHub;

  /**
   * 为请求头用户建立 SSE 通知连接；可选 userIdParam 存在时必须与请求头用户一致.
   *
   * @param userId 由 X-User-Id 请求头传入的用户编号
   * @param userIdParam 可选的订阅用户编号查询参数
   * @return 注册成功的 SSE 发射器
   */
  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      @RequestHeader("X-User-Id") Long userId, @RequestParam(required = false) Long userIdParam) {
    return sseHub.register(userId, userIdParam);
  }
}
