package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.notify.sse.SseHub;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SseControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/notify/sse")
@RequiredArgsConstructor
public class SseControllerV1 {

    private final SseHub sseHub;


    /**
     * 执行 subscribe 方法。
     * @param userId userId 参数
     * @param userIdParam userIdParam 参数
     * @return 处理结果
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestHeader("X-User-Id") Long userId,
                                @RequestParam(required = false) Long userIdParam) {
        return sseHub.register(userId, userIdParam);
    }
}
