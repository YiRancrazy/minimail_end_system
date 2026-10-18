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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SseHub 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
public class SseHub {


    private static final int MAX_CONNECTIONS_PER_USER = 5;


    private static final int MAX_GLOBAL_CONNECTIONS = 1000;


    private static final long CONNECTION_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final int maxGlobalConnections;

    private final long connectionTimeoutMillis;

    private final ConcurrentHashMap<Long, Set<SseConnection>> emitters = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    private final AtomicInteger globalConnectionCount = new AtomicInteger();

    private final ScheduledExecutorService cleanupExecutor;


    /**
     * 构造 SseHub 实例。
     */
    public SseHub() {
        this(MAX_GLOBAL_CONNECTIONS, CONNECTION_TIMEOUT_MILLIS);
    }


    /**
     * 构造 SseHub 实例。
     * @param maxGlobalConnections maxGlobalConnections
     * @param connectionTimeoutMillis connectionTimeoutMillis
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
     * 执行 register 方法。
     * @param userId userId 参数
     * @return 处理结果
     */
    public SseEmitter register(Long userId) {
        if (!tryAcquireUser(userId)) {
            log.warn("sse 连接数超过限制，userId={}，max={}", userId, MAX_CONNECTIONS_PER_USER);
            throw new BizException(NotifyCodeEnum.NOTIFY_SSE_LIMIT);
        }
        if (!tryAcquireGlobal()) {
            releaseUser(userId);
            log.warn("sse 全局连接数超过限制，userId={}，max={}", userId, maxGlobalConnections);
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
            log.warn("sse ready 事件发送失败，回滚连接注册，userId={}", userId, e);
            remove(connection);
            emitter.completeWithError(e);
            throw new BizException(NotifyCodeEnum.NOTIFY_SSE_CONNECT_FAILED);
        }
        return emitter;
    }


    /**
     * 创建Emitter。
     * @return 处理结果
     */
    SseEmitter createEmitter() {
        return new SseEmitter(connectionTimeoutMillis);
    }


    /**
     * 发送业务消息。
     * @param userId userId 参数
     * @param payload payload 参数
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
                log.debug("sse 发送失败，连接已移除，userId={}", userId, ex);
                remove(connection);
            }
        }
    }


    /**
     * 处理调度IdleCleanup。
     * @param connection connection
     */
    private void scheduleIdleCleanup(SseConnection connection) {
        ScheduledFuture<?> cleanupFuture = cleanupExecutor.schedule(
                () -> close(connection), connectionTimeoutMillis, TimeUnit.MILLISECONDS);
        connection.setCleanupFuture(cleanupFuture);
    }


    /**
     * 释放当前资源。
     * @param connection connection
     */
    private void close(SseConnection connection) {
        if (!remove(connection)) {
            return;
        }
        try {
            connection.emitter.complete();
        }
        catch (RuntimeException e) {
            log.debug("连接移除后 sse complete 失败，userId={}", connection.userId, e);
        }
    }


    /**
     * 删除remove。
     * @param connection connection
     * @return 是否满足条件
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
     * 执行 register 方法。
     * @param userId userId 参数
     * @param userIdParam userIdParam 参数
     * @return 处理结果
     */
    public SseEmitter register(Long userId, Long userIdParam) {
        if (userIdParam != null && !userId.equals(userIdParam)) {
            log.warn("sse 订阅被拒绝，headerUserId={}，paramUserId={}", userId, userIdParam);
            throw new BizException(NotifyCodeEnum.NOTIFY_SSE_FORBIDDEN);
        }
        return register(userId);
    }


    /**
     * 处理tryAcquire用户。
     * @param userId 用户 ID
     * @return 是否满足条件
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
     * 处理tryAcquireGlobal。
     * @return 是否满足条件
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
     * 执行用户操作。
     * @param userId 用户 ID
     */
    private void releaseUser(Long userId) {
        AtomicInteger count = connectionCounts.get(userId);
        if (count != null) {
            count.decrementAndGet();
        }
    }


    /**
     * 执行Global操作。
     */
    private void releaseGlobal() {
        int remaining = globalConnectionCount.decrementAndGet();
        if (remaining < 0) {
            log.error("sse 全局连接计数变为负数");
        }
    }


    /**
     * 处理shutdown。
     */
    @PreDestroy
    void shutdown() {
        cleanupExecutor.shutdownNow();
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: SseConnection 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    private static final class SseConnection {

        private final Long userId;

        private final SseEmitter emitter;

        private volatile ScheduledFuture<?> cleanupFuture;

        /**
         * 构造 SseConnection 实例。
         * @param userId 用户 ID
         * @param emitter emitter
         */
        private SseConnection(Long userId, SseEmitter emitter) {
            this.userId = userId;
            this.emitter = emitter;
        }


        /**
         * 更新CleanupFuture。
         * @param cleanupFuture cleanupFuture
         */
        private void setCleanupFuture(ScheduledFuture<?> cleanupFuture) {
            this.cleanupFuture = cleanupFuture;
        }


        /**
         * 处理取消Cleanup。
         */
        private void cancelCleanup() {
            ScheduledFuture<?> future = cleanupFuture;
            if (future != null) {
                future.cancel(false);
            }
        }
    }
}
