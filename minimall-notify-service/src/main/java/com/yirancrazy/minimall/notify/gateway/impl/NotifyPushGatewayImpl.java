package com.yirancrazy.minimall.notify.gateway.impl;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.notify.gateway.NotifyPushGateway;
import com.yirancrazy.minimall.notify.sse.SseHub;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyPushGatewayImpl 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
@RequiredArgsConstructor
public class NotifyPushGatewayImpl implements NotifyPushGateway {

    private final SseHub sseHub;

    /**
     * 发送业务消息。
     * @param userId userId 参数
     * @param payload payload 参数
     */
    public void send(Long userId, String payload) {
        sseHub.send(userId, payload);
    }
}
