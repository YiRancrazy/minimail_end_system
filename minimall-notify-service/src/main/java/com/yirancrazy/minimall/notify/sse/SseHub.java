package com.yirancrazy.minimall.notify.sse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Iter-2 push 模型：每用户最多 1 条 SseEmitter 长连接。新连接覆盖旧连接。
 * Iter-3 替换为真正的多端推送（WebSocket/Union Tech Push）。
 */
@RestController
@RequestMapping("/internal/notify/sse")
@Component
public class SseHub {

    private final ConcurrentHashMap<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 为指定用户创建 SseEmitter 长连接并加入连接中心，完成/超时/异常时自动清理。
     *
     * @param userId 订阅用户主键
     * @return 已初始化并发送 ready 事件的 SseEmitter
     */
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> { emitter.complete(); remove(userId, emitter); });
        emitter.onError(t -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("connected"));
        } catch (IOException ignored) {}
        return emitter;
    }

    /**
     * 向目标用户的所有在线连接推送 notify 事件，发送失败的连接自动清理。
     *
     * @param userId 目标用户主键
     * @param payload 通知内容负载
     */
    public void send(Long userId, String payload) {
        Set<SseEmitter> set = emitters.get(userId);
        if (set == null) return;
        for (SseEmitter e : set) {
            try {
                e.send(SseEmitter.event().name("notify").data(payload));
            } catch (IOException ex) {
                remove(userId, e);
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(userId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emitters.remove(userId, set);
            }
        }
    }

    /**
     * SSE 连接入口，接收前端订阅请求并复用 register 逻辑返回长连接 emitter。
     *
     * @param userId 订阅用户主键
     * @return 用于流式响应的 SseEmitter
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter open(@RequestParam Long userId) {
        return register(userId);
    }
}