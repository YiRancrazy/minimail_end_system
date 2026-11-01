package com.yirancrazy.minimall.order.schedule;

import com.xxl.job.core.handler.annotation.XxlJob;
import com.yirancrazy.minimall.order.service.CartCleanupService;
import com.yirancrazy.minimall.order.service.OrderOutboxPublisher;
import com.yirancrazy.minimall.order.service.OrderService;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在 XXL-Job 启用时提供订单补偿任务的 JobHandler 入口。
 *   业务边界：任务入口使用 Redis 短锁互斥；超时、确认、库存、购物车和发件箱业务仍委托各服务执行。
 *   使用场景：XXL-Job 调度中心配置并触发订单任务时使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "minimall.xxl-job.enabled", havingValue = "true")
public class OrderXxlJobHandler {

    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private final OrderService orderService;
    private final CartCleanupService cartCleanupService;
    private final OrderOutboxPublisher orderOutboxPublisher;
    private final StringRedisTemplate redisTemplate;
    private final int outboxBatchSize;

    /**
     * 创建 XXL-Job 订单任务处理器。
     *
     * <p>注入业务服务、发件箱发布器、Redis 和批量大小配置。
     *
     * @param orderService 订单业务服务
     * @param cartCleanupService 购物车清理任务立即执行和扫描服务
     * @param orderOutboxPublisher 待发送订单事件批量发布器
     * @param redisTemplate 分布式任务锁使用的 Redis 模板
     * @param outboxBatchSize 发件箱单批发布数量
     */
    public OrderXxlJobHandler(
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
    public void scanExpiredOrders() {
        String lockKey = "order:schedule:scanExpiredOrders";
        if (!tryLock(lockKey)) {
            log.info("xxl-job scanExpiredOrders 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = orderService.scanExpiredOrders();
            log.info("xxl-job scanExpiredOrders 执行完成，processed={}", count);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 由调度器触发已发货订单自动确认。
     *
     * <p>使用独立 Redis 锁，避免多实例重复推进订单状态。
     */
    public void scanAutoConfirm() {
        String lockKey = "order:schedule:scanAutoConfirm";
        if (!tryLock(lockKey)) {
            log.info("xxl-job scanAutoConfirm 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = orderService.scanAutoConfirm();
            log.info("xxl-job scanAutoConfirm 执行完成，processed={}", count);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 由调度器触发待释放库存补偿。
     *
     * <p>使用独立 Redis 锁并按批扫描，将具体释放逻辑交给 OrderService。
     */
    public void scanStockReleasePending() {
        String lockKey = "order:schedule:scanStockReleasePending";
        if (!tryLock(lockKey)) {
            log.info("xxl-job scanStockReleasePending 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = orderService.scanStockReleasePending();
            log.info("xxl-job scanStockReleasePending 执行完成，processed={}", count);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 由调度器触发购物车清理任务扫描。
     *
     * <p>获取 Redis 锁后调用 CartCleanupService，锁占用时直接跳过本轮。
     */
    public void scanCartCleanupPending() {
        String lockKey = "order:schedule:scanCartCleanupPending";
        if (!tryLock(lockKey)) {
            log.info("xxl-job scanCartCleanupPending 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = cartCleanupService.scanPending();
            log.info("xxl-job scanCartCleanupPending 执行完成，processed={}", count);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 由调度器转发票箱待发送事件。
     *
     * <p>获取 Redis 锁后按配置批量调用发布器，成功发送数量用于日志和监控。
     */
    @XxlJob("orderOutboxRelay")
    public void relayOrderOutbox() {
        String lockKey = "order:schedule:relayOrderOutbox";
        if (!tryLock(lockKey)) {
            log.info("xxl-job relayOrderOutbox 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = orderOutboxPublisher.publishPending(outboxBatchSize);
            log.info("xxl-job relayOrderOutbox 执行完成，sent={}", count);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 尝试获取 XXL-Job 任务锁。
     *
     * <p>以固定值 1 和 60 秒 TTL 写入 Redis，返回是否成功。
     *
     * @param key Redis 任务锁键
     * @return 成功获取时返回 true。
     */
    private boolean tryLock(String key) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL);
        return Boolean.TRUE.equals(locked);
    }
}
