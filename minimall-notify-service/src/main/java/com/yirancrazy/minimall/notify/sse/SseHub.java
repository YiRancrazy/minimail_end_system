package com.yirancrazy.minimall.notify.sse;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SseHub，提供 SSE 长连接订阅与实时推送能力
 * @Version: 1.4
 * @DateTime: 2026/08/16
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notify/sse")
public class SseHub {

    /** 单用户最大并发 SSE 连接数，超限拒绝新连接，防止连接耗尽型 DoS */
    private static final int MAX_CONNECTIONS_PER_USER = 5;

    /** 单实例最大并发 SSE 连接数，避免单账号限制被大量用户绕过 */
    private static final int MAX_GLOBAL_CONNECTIONS = 1000;

    /** SSE 连接最长空闲时间，超时后由服务端主动回收 */
    private static final long CONNECTION_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final int maxGlobalConnections;

    private final long connectionTimeoutMillis;

    private final ConcurrentHashMap<Long, Set<SseConnection>> emitters = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    private final AtomicInteger globalConnectionCount = new AtomicInteger();

    private final ScheduledExecutorService cleanupExecutor;

    /**
     * 创建使用默认连接上限与超时时间的 SSE 连接中心。
     */
    public SseHub() {
        this(MAX_GLOBAL_CONNECTIONS, CONNECTION_TIMEOUT_MILLIS);
    }

    /**
     * 创建可配置连接上限与超时时间的 SSE 连接中心，便于测试边界行为。
     *
     * @param maxGlobalConnections 全局最大连接数
     * @param connectionTimeoutMillis 连接最长空闲时间（毫秒）
     */
    SseHub(int maxGlobalConnections, long connectionTimeoutMillis) {
        this.maxGlobalConnections = maxGlobalConnections;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "sse-idle-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        this.cleanupExecutor = executor;
    }

    /**
     * 为指定用户创建 SseEmitter 长连接并加入连接中心，完成/超时/异常时自动清理。
     * 超过单用户或全局连接上限时拒绝注册，防止连接耗尽型 DoS。
     *
     * @param userId 订阅用户主键
     * @return 已初始化并发送 ready 事件的 SseEmitter
     * @throws BizException 当单用户或全局连接数已达上限时
     */
    public SseEmitter register(Long userId) {
        if (!tryAcquireUser(userId)) {
            log.warn("sse connection limit exceeded, userId={}, max={}", userId, MAX_CONNECTIONS_PER_USER);
            throw new BizException(NotifyCodeEnum.NOTIFY_SSE_LIMIT);
        }
        if (!tryAcquireGlobal()) {
            releaseUser(userId);
            log.warn("sse global connection limit exceeded, userId={}, max={}", userId, maxGlobalConnections);
            throw new BizException(NotifyCodeEnum.NOTIFY_SSE_LIMIT);
        }
        SseEmitter emitter = createEmitter();
        SseConnection connection = new SseConnection(userId, emitter);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(connection);
        emitter.onCompletion(() -> remove(connection));
        emitter.onTimeout(() -> close(connection));
        emitter.onError(t -> remove(connection));
        scheduleIdleCleanup(connection);
        try {
            emitter.send(SseEmitter.event().name("ready").data("connected"));
        }
        catch (Exception e) {
            // ready 事件失败说明连接不可用：回滚注册并释放配额，避免僵尸连接长期占用连接数
            log.warn("sse ready event failed, rollback registration, userId={}", userId, e);
            remove(connection);
            emitter.completeWithError(e);
            throw new BizException(NotifyCodeEnum.NOTIFY_SSE_CONNECT_FAILED);
        }
        return emitter;
    }

    /**
     * 创建 SSE 连接对象，单测可覆写该方法注入可控 emitter。
     * @return 新的 SSE 连接
     */
    SseEmitter createEmitter() {
        return new SseEmitter(connectionTimeoutMillis);
    }

    /**
     * 向目标用户的所有在线连接推送 notify 事件，发送失败的连接自动清理。
     *
     * @param userId 目标用户主键
     * @param payload 通知内容负载
     */
    public void send(Long userId, String payload) {
        Set<SseConnection> set = emitters.get(userId);
        if (set == null) {
            return;
        }
        for (SseConnection connection : set) {
            try {
                connection.emitter.send(SseEmitter.event().name("notify").data(payload));
            }
            catch (IOException | RuntimeException ex) {
                // 连接已断开或 emitter 已完成：任意发送异常都移除连接并释放配额
                log.debug("sse send failed, connection removed, userId={}", userId, ex);
                remove(connection);
            }
        }
    }

    /**
     * 定时关闭空闲连接，完成/异常回调会通过同一移除路径释放两类配额。
     *
     * @param connection SSE 连接记录
     */
    private void scheduleIdleCleanup(SseConnection connection) {
        ScheduledFuture<?> cleanupFuture = cleanupExecutor.schedule(
                () -> close(connection), connectionTimeoutMillis, TimeUnit.MILLISECONDS);
        connection.setCleanupFuture(cleanupFuture);
    }

    /**
     * 关闭连接并释放配额。
     *
     * @param connection SSE 连接记录
     */
    private void close(SseConnection connection) {
        if (!remove(connection)) {
            return;
        }
        try {
            connection.emitter.complete();
        }
        catch (RuntimeException e) {
            log.debug("sse complete failed after connection removal, userId={}", connection.userId, e);
        }
    }

    /**
     * 从连接中心移除连接并释放用户、全局配额。
     *
     * @param connection SSE 连接记录
     * @return 本次调用是否完成首次移除
     */
    private boolean remove(SseConnection connection) {
        Set<SseConnection> set = emitters.get(connection.userId);
        boolean removed = set != null && set.remove(connection);
        if (!removed) {
            return false;
        }
        connection.cancelCleanup();
        if (set.isEmpty()) {
            emitters.remove(connection.userId, set);
        }
        releaseUser(connection.userId);
        releaseGlobal();
        return true;
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
    private boolean tryAcquireUser(Long userId) {
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

    /**
     * 以 CAS 方式占用全局连接配额，保证所有用户合计不超过全局上限。
     *
     * @return 占用成功返回 true；已达全局上限返回 false
     */
    private boolean tryAcquireGlobal() {
        while (true) {
            int current = globalConnectionCount.get();
            if (current >= maxGlobalConnections) {
                return false;
            }
            if (globalConnectionCount.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    /**
     * 释放单用户连接配额。
     *
     * @param userId 订阅用户主键
     */
    private void releaseUser(Long userId) {
        AtomicInteger count = connectionCounts.get(userId);
        if (count != null) {
            count.decrementAndGet();
        }
    }

    /**
     * 释放全局连接配额。
     */
    private void releaseGlobal() {
        int remaining = globalConnectionCount.decrementAndGet();
        if (remaining < 0) {
            log.error("sse global connection count became negative");
        }
    }

    /**
     * 应用关闭时停止空闲连接回收线程。
     */
    @PreDestroy
    void shutdown() {
        cleanupExecutor.shutdownNow();
    }

    /**
     * SSE 连接记录，集中保存用户、emitter 与定时回收任务。
     */
    private static final class SseConnection {

        private final Long userId;

        private final SseEmitter emitter;

        private volatile ScheduledFuture<?> cleanupFuture;

        private SseConnection(Long userId, SseEmitter emitter) {
            this.userId = userId;
            this.emitter = emitter;
        }

        /**
         * 保存空闲回收任务，便于连接提前结束时取消。
         *
         * @param cleanupFuture 定时回收任务
         */
        private void setCleanupFuture(ScheduledFuture<?> cleanupFuture) {
            this.cleanupFuture = cleanupFuture;
        }

        /**
         * 取消尚未执行的空闲回收任务。
         */
        private void cancelCleanup() {
            ScheduledFuture<?> future = cleanupFuture;
            if (future != null) {
                future.cancel(false);
            }
        }
    }
}
