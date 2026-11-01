package com.yirancrazy.minimall.notify.gateway.impl;

import com.yirancrazy.minimall.notify.gateway.NotifyPushGateway;
import com.yirancrazy.minimall.notify.sse.SseHub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 将通知推送网关的调用委托给 SseHub，由 SseHub 管理连接、限流和失效连接清理.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
@RequiredArgsConstructor
public class NotifyPushGatewayImpl implements NotifyPushGateway {

    private final SseHub sseHub;

    /**
     * 将通知标题推送到指定用户当前已建立的 SSE 连接.
     *
     * @param userId 接收通知的用户编号
     * @param payload 实时推送的展示内容
     */
    public void send(Long userId, String payload) {
        sseHub.send(userId, payload);
    }
}
