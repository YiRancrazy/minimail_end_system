package com.yirancrazy.minimall.order.schedule;

import com.yirancrazy.minimall.order.service.CartCleanupService;
import com.yirancrazy.minimall.order.service.OrderOutboxPublisher;
import com.yirancrazy.minimall.order.service.OrderService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在 Spring Scheduling 开启且 XXL-Job 关闭时执行订单补偿扫描。
 *   业务边界：使用 Redis 分布式锁保证单实例执行，覆盖超时取消、自动确认、库存释放、购物车清理和发件箱转发。
 *   使用场景：单机或 Spring Scheduling 部署模式加载。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
// XXL-Job 启用时禁用本套定时任务，双轨互斥防止同一扫描被两套调度器重复触发
@ConditionalOnExpression(
        "'${minimall.schedule.enabled:false}' == 'true' "
                + "and '${minimall.xxl-job.enabled:false}' != 'true'")
public class OrderScheduleHandler {

    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT =
            RedisScript.of(
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else"
                            + " return 0 end",
                    Long.class);

    private final OrderService orderService;
    private final CartCleanupService cartCleanupService;
    private final OrderOutboxPublisher orderOutboxPublisher;
    private final StringRedisTemplate redisTemplate;
    private final int outboxBatchSize;

    /**
     * 创建 Spring Scheduling 订单任务处理器。
     *
     * <p>注入订单、购物车、发件箱和 Redis 依赖，并接收发件箱单批大小配置。
     *
     * @param orderService 订单业务服务
     * @param cartCleanupService 购物车清理任务立即执行和扫描服务
     * @param orderOutboxPublisher 待发送订单事件批量发布器
     * @param redisTemplate 分布式任务锁使用的 Redis 模板
     * @param outboxBatchSize 发件箱单批发布数量
     */
    public OrderScheduleHandler(
            OrderService orderService,
            CartCleanupService cartCleanupService,
            OrderOutboxPublisher orderOutboxPublisher,
            StringRedisTemplate redisTemplate,
            @Value("${minimall.outbox.batch-size:100}") int outboxBatchSize) {
        this.orderService = orderService;
        this.cartCleanupService = cartCleanupService;
        this.orderOutboxPublisher = orderOutboxPublisher;
        this.redisTemplate = redisTemplate;
        this.outboxBatchSize = outboxBatchSize;
    }

    /**
     * 由调度器触发支付超时订单扫描。
     *
     * <p>使用 Redis 锁保证单实例执行，业务处理委托 OrderService。
     */
    @Scheduled(
            fixedDelayString = "${minimall.schedule.order.scan-expired-orders-ms}",
            initialDelayString = "${minimall.schedule.order.scan-init-ms}")
    public void scanExpiredOrders() {
        String lockKey = "order:schedule:scanExpiredOrders";
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            log.debug("scanExpiredOrders 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = orderService.scanExpiredOrders();
            log.debug("scanExpiredOrders 执行完成，processed={}", count);
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 由调度器触发已发货订单自动确认。
     *
     * <p>使用独立 Redis 锁，避免多实例重复推进订单状态。
     */
    @Scheduled(
            fixedDelayString = "${minimall.schedule.order.scan-auto-confirm-ms}",
            initialDelayString = "${minimall.schedule.order.scan-init-ms}")
    public void scanAutoConfirm() {
        String lockKey = "order:schedule:scanAutoConfirm";
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            log.debug("scanAutoConfirm 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = orderService.scanAutoConfirm();
            log.debug("scanAutoConfirm 执行完成，processed={}", count);
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 由调度器触发待释放库存补偿。
     *
     * <p>使用独立 Redis 锁并按批扫描，将具体释放逻辑交给 OrderService。
     */
    @Scheduled(
            fixedDelayString = "${minimall.schedule.order.scan-stock-release-pending-ms}",
            initialDelayString = "${minimall.schedule.order.scan-init-ms}")
    public void scanStockReleasePending() {
        String lockKey = "order:schedule:scanStockReleasePending";
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            log.debug("scanStockReleasePending 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = orderService.scanStockReleasePending();
            log.debug("scanStockReleasePending 执行完成，processed={}", count);
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 由调度器触发购物车清理任务扫描。
     *
     * <p>获取 Redis 锁后调用 CartCleanupService，锁占用时直接跳过本轮。
     */
    @Scheduled(
            fixedDelayString = "${minimall.schedule.order.scan-cart-cleanup-ms}",
            initialDelayString = "${minimall.schedule.order.scan-init-ms}")
    public void scanCartCleanupPending() {
        String lockKey = "order:schedule:scanCartCleanupPending";
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            log.debug("scanCartCleanupPending 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = cartCleanupService.scanPending();
            log.debug("scanCartCleanupPending 执行完成，processed={}", count);
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 由调度器转发票箱待发送事件。
     *
     * <p>获取 Redis 锁后按配置批量调用发布器，成功发送数量用于日志和监控。
     */
    @Scheduled(
            fixedDelayString = "${minimall.schedule.order.scan-order-outbox-ms}",
            initialDelayString = "${minimall.schedule.order.scan-init-ms}")
    public void relayOrderOutbox() {
        String lockKey = "order:schedule:relayOrderOutbox";
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            log.debug("relayOrderOutbox 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = orderOutboxPublisher.publishPending(outboxBatchSize);
            log.debug("relayOrderOutbox 执行完成，sent={}", count);
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 尝试获取 Spring Scheduling 任务锁。
     *
     * <p>使用随机 UUID 作为持有者令牌并按 10 分钟 TTL 写入 Redis；未获取时返回 null。
     *
     * @param key Redis 任务锁键
     * @return 锁令牌，或 null 表示锁已被占用。
     */
    private String tryLock(String key) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL);
        return Boolean.TRUE.equals(locked) ? token : null;
    }

    /**
     * 安全释放当前实例持有的 Redis 锁。
     *
     * <p>Lua 脚本仅在校验 token 一致时删除，释放失败只记录告警。
     *
     * @param key Redis 任务锁键
     * @param token 当前实例持有锁时写入的令牌
     */
    private void releaseLock(String key, String token) {
        try {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(key), token);
        } catch (Exception e) {
            log.warn("释放定时任务锁失败，key={}", key, e);
        }
    }
}
