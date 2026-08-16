package com.yirancrazy.minimall.order.schedule;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: XXL-Job 任务处理器，仅在 minimall.xxl-job.enabled=true 时启用。复用 OrderService 既有定时逻辑，与 Spring @Scheduled 双轨制。
 * @Version: 1.1
 * @DateTime: 2026/08/05
 **/
@Slf4j
@Component
@ConditionalOnProperty(name = "minimall.xxl-job.enabled", havingValue = "true")
public class OrderXxlJobHandler {

    /** 分布式锁 TTL：扫描任务最长执行时长兜底，防止持有节点宕机后锁永不过期 */
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;

    public OrderXxlJobHandler(OrderService orderService, StringRedisTemplate redisTemplate) {
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 扫描超时未支付订单，由 XXL-Job 调度；SETNX 加锁失败说明其他实例正在执行，直接跳过。
     */
    @XxlJob("scanExpiredOrders")
    public void scanExpiredOrders() {
        String lockKey = "order:schedule:scanExpiredOrders";
        if (!tryLock(lockKey)) {
            log.info("xxl-job scanExpiredOrders skipped, lock held by another instance");
            return;
        }
        try {
            int count = orderService.scanExpiredOrders();
            log.info("xxl-job scanExpiredOrders, processed={}", count);
        }
        finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 扫描发货后超期未确认收货订单，由 XXL-Job 调度；SETNX 加锁失败说明其他实例正在执行，直接跳过。
     */
    @XxlJob("scanAutoConfirm")
    public void scanAutoConfirm() {
        String lockKey = "order:schedule:scanAutoConfirm";
        if (!tryLock(lockKey)) {
            log.info("xxl-job scanAutoConfirm skipped, lock held by another instance");
            return;
        }
        try {
            int count = orderService.scanAutoConfirm();
            log.info("xxl-job scanAutoConfirm, processed={}", count);
        }
        finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * Redis SETNX 加锁，防止多实例并发执行同一扫描任务。未做锁值与删除的原子比对：
     * 仅当单次扫描超过 TTL 60s 才可能误删他人锁，权衡加 Lua 的复杂度后维持简化实现。
     * @param key 锁键
     * @return true 表示获取锁成功
     */
    private boolean tryLock(String key) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL);
        return Boolean.TRUE.equals(locked);
    }
}
