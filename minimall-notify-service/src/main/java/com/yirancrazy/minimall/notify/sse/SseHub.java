package com.yirancrazy.minimall.notify.sse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SseHub，提供 SSE 长连接订阅与实时推送能力
 * @Version: 1.2
 * @DateTime: 2026/08/16
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notify/sse")
public class SseHub {

    /** 单用户最大并发 SSE 连接数，超限拒绝新连接，防止连接耗尽型 DoS */
    private static final int MAX_CONNECTIONS_PER_USER = 5;

    private final ConcurrentHashMap<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    /**
     * 为指定用户创建 SseEmitter 长连接并加入连接中心，完成/超时/异常时自动清理。
     * 超过单用户连接上限时拒绝注册，防止单账号无限建立连接耗尽服务资源。
     *
     * @param userId 订阅用户主键
     * @return 已初始化并发送 ready 事件的 SseEmitter
     * @throws BizException 当单用户连接数已达上限时
     */
    public SseEmitter register(Long userId) {
        if (!tryAcquire(userId)) {
            log.warn("sse connection limit exceeded, userId={}, max={}", userId, MAX_CONNECTIONS_PER_USER);
            throw new BizException(NotifyCodeEnum.NOTIFY_SSE_LIMIT);
        }
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            remove(userId, emitter);
        });
        emitter.onError(t -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("connected"));
        }
        // ready 事件尽力而为：未绑定 response（如单测直调）或连接已关闭时忽略失败，不影响注册
        catch (Exception ignored) {
        }
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
        if (set == null) {
            return;
        }
        for (SseEmitter e : set) {
            try {
                e.send(SseEmitter.event().name("notify").data(payload));
            }
            catch (IOException ex) {
                remove(userId, e);
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(userId);
        boolean removed = set != null && set.remove(emitter);
        if (removed && set.isEmpty()) {
            emitters.remove(userId, set);
        }
        // 仅首次移除时释放连接配额，防止 onCompletion/onTimeout/onError 重复回调导致多减
        if (removed) {
            AtomicInteger count = connectionCounts.get(userId);
            if (count != null) {
                count.decrementAndGet();
            }
        }
    }

    /**
     * SSE 连接入口，接收前端订阅请求并复用 register 逻辑返回长连接 emitter。
     * 订阅归属以网关注入的 X-User-Id 为准，请求参数 userId 必须与之匹配，防止订阅他人实时推送。
     *
     * @param userId 当前登录用户ID（Header 注入）
     * @param userIdParam 前端透传的订阅用户ID，用于兼容旧调用；与 userId 不一致时拒绝
     * @return 用于流式响应的 SseEmitter
     * @throws BizException 当订阅归属不匹配或连接数超限时
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter open(@RequestHeader("X-User-Id") Long userId,
                           @RequestParam(required = false) Long userIdParam) {
        if (userIdParam != null && !userId.equals(userIdParam)) {
            log.warn("sse subscribe denied, headerUserId={}, paramUserId={}", userId, userIdParam);
            throw new BizException(NotifyCodeEnum.NOTIFY_SSE_FORBIDDEN);
        }
        return register(userId);
    }

    /**
     * 以 CAS 方式占用单用户连接配额，并发安全地保证不超过上限。
     *
     * @param userId 订阅用户主键
     * @return 占用成功返回 true；已达上限返回 false
     */
    private boolean tryAcquire(Long userId) {
        AtomicInteger count = connectionCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        while (true) {
            int current = count.get();
            if (current >= MAX_CONNECTIONS_PER_USER) {
                return false;
            }
            if (count.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }
}
