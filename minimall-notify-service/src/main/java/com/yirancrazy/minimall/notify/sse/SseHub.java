package com.yirancrazy.minimall.notify.sse;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 线程安全地维护用户 SSE 连接，限制单用户 5 条和全局默认 1000 条连接，并通过单线程调度器回收超时或发送失败的连接.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
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

  /** 使用默认全局连接上限和 30 分钟连接超时构造生产用 SSE 管理器. */
  public SseHub() {
    this(MAX_GLOBAL_CONNECTIONS, CONNECTION_TIMEOUT_MILLIS);
  }

  /**
   * 使用指定全局连接上限和超时时间构造 SSE 管理器，并创建单线程 daemon 清理调度器.
   *
   * @param maxGlobalConnections 全局 SSE 连接上限
   * @param connectionTimeoutMillis 单连接空闲超时时间，单位毫秒
   */
  SseHub(int maxGlobalConnections, long connectionTimeoutMillis) {
    this.maxGlobalConnections = maxGlobalConnections;
    this.connectionTimeoutMillis = connectionTimeoutMillis;
    ScheduledThreadPoolExecutor executor =
        new ScheduledThreadPoolExecutor(
            1,
            runnable -> {
              Thread thread = new Thread(runnable, "sse-idle-cleanup");
              thread.setDaemon(true);
              return thread;
            });
    executor.setRemoveOnCancelPolicy(true);
    this.cleanupExecutor = executor;
  }

  /**
   * 在单用户和全局配额内注册 SSE 连接，绑定完成、超时和错误回调，并发送 ready 事件；ready 失败会回滚配额与连接.
   *
   * @param userId 接收通知的用户编号
   * @return 已注册的 SSE 发射器
   * @throws BizException 单用户或全局连接达到上限，或 ready 事件无法发送时抛出
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
    } catch (Exception e) {
      // ready 事件失败说明连接不可用：回滚注册并释放配额，避免僵尸连接长期占用连接数
      log.warn("sse ready 事件发送失败，回滚连接注册，userId={}", userId, e);
      remove(connection);
      emitter.completeWithError(e);
      throw new BizException(NotifyCodeEnum.NOTIFY_SSE_CONNECT_FAILED);
    }
    return emitter;
  }

  /**
   * 校验可选订阅用户编号与请求头用户一致后注册连接；参数为空时接受请求头用户.
   *
   * @param userId 由可信请求头传入的用户编号
   * @param userIdParam 可选的查询参数用户编号
   * @return 已注册的 SSE 发射器
   * @throws BizException 查询参数用户与请求头用户不一致，或底层注册失败时抛出
   */
  public SseEmitter register(Long userId, Long userIdParam) {
    if (userIdParam != null && !userId.equals(userIdParam)) {
      log.warn("sse 订阅被拒绝，headerUserId={}，paramUserId={}", userId, userIdParam);
      throw new BizException(NotifyCodeEnum.NOTIFY_SSE_FORBIDDEN);
    }
    return register(userId);
  }

  /**
   * 创建使用当前连接超时配置的 SSE 发射器；保留为可覆写方法供连接异常场景测试替换.
   *
   * @return 新的 SSE 发射器
   */
  SseEmitter createEmitter() {
    return new SseEmitter(connectionTimeoutMillis);
  }

  /**
   * 向用户当前全部 SSE 连接发送 notify 事件；单条连接发送异常时移除该连接并继续处理其他连接.
   *
   * @param userId 目标用户编号
   * @param payload 推送内容
   */
  public void send(Long userId, String payload) {
    Set<SseConnection> set = emitters.get(userId);
    if (set == null) {
      return;
    }
    for (SseConnection connection : set) {
      try {
        connection.emitter.send(SseEmitter.event().name("notify").data(payload));
      } catch (IOException | RuntimeException ex) {
        // 连接已断开或 emitter 已完成：任意发送异常都移除连接并释放配额
        log.debug("sse 发送失败，连接已移除，userId={}", userId, ex);
        remove(connection);
      }
    }
  }

  /**
   * 为连接安排一次空闲超时回收任务，并把任务句柄保存到连接上以便移除时取消.
   *
   * @param connection 需要安排超时回收的连接
   */
  private void scheduleIdleCleanup(SseConnection connection) {
    ScheduledFuture<?> cleanupFuture =
        cleanupExecutor.schedule(
            () -> close(connection), connectionTimeoutMillis, TimeUnit.MILLISECONDS);
    connection.setCleanupFuture(cleanupFuture);
  }

  /**
   * 先尝试从容器移除连接；仅在移除成功时完成发射器，避免重复完成其他回调已清理的连接.
   *
   * @param connection 超时或主动关闭的连接
   */
  private void close(SseConnection connection) {
    if (!remove(connection)) {
      return;
    }
    try {
      connection.emitter.complete();
    } catch (RuntimeException e) {
      log.debug("连接移除后 sse complete 失败，userId={}", connection.userId, e);
    }
  }

  /**
   * 原子地移除指定连接、取消超时任务、清理空用户集合并释放单用户与全局配额.
   *
   * @param connection 待移除连接
   * @return true 表示本次调用真正移除了连接；重复移除返回 false
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
   * 通过 CAS 循环为指定用户占用一个连接配额，达到单用户上限时返回 false.
   *
   * @param userId 用户编号
   * @return true 表示成功占用配额
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
   * 通过 CAS 循环占用一个全局连接配额，达到配置上限时返回 false.
   *
   * @return true 表示成功占用全局配额
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
   * 释放指定用户的一个连接计数；计数不存在时不做处理.
   *
   * @param userId 用户编号
   */
  private void releaseUser(Long userId) {
    AtomicInteger count = connectionCounts.get(userId);
    if (count != null) {
      count.decrementAndGet();
    }
  }

  /** 释放一个全局连接计数；计数变为负数时记录错误用于发现重复释放. */
  private void releaseGlobal() {
    int remaining = globalConnectionCount.decrementAndGet();
    if (remaining < 0) {
      log.error("sse 全局连接计数变为负数");
    }
  }

  /** 应用销毁时立即停止连接清理调度器，不再接受新的空闲回收任务. */
  @PreDestroy
  void shutdown() {
    cleanupExecutor.shutdownNow();
  }

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 持有单个 SSE 连接的用户编号、发射器和空闲回收任务句柄，供 SseHub 内部并发管理.
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  private static final class SseConnection {

    private final Long userId;

    private final SseEmitter emitter;

    private volatile ScheduledFuture<?> cleanupFuture;

    /**
     * 绑定用户和发射器；超时任务由注册流程随后设置.
     *
     * @param userId 连接所属用户编号
     * @param emitter 对外返回的 SSE 发射器
     */
    private SseConnection(Long userId, SseEmitter emitter) {
      this.userId = userId;
      this.emitter = emitter;
    }

    /**
     * 保存当前连接的空闲回收任务句柄，供移除连接时取消.
     *
     * @param cleanupFuture 已安排的回收任务
     */
    private void setCleanupFuture(ScheduledFuture<?> cleanupFuture) {
      this.cleanupFuture = cleanupFuture;
    }

    /** 取消当前连接的空闲回收任务；任务尚未设置时不做处理. */
    private void cancelCleanup() {
      ScheduledFuture<?> future = cleanupFuture;
      if (future != null) {
        future.cancel(false);
      }
    }
  }
}
