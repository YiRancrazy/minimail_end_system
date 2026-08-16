package com.yirancrazy.minimall.order.schedule;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单定时任务调度器，触发超时取消与自动确认收货扫描。ponytail: 用 Spring @Scheduled 替代 XXL-Job，等部署 XXL-Job admin 再切换。
 * @Version: 1.1
 * @DateTime: 2026/08/05
 **/
@Slf4j
@Component
// XXL-Job 启用时禁用本套定时任务，双轨互斥防止同一扫描被两套调度器重复触发
@ConditionalOnExpression("'${minimall.schedule.enabled:false}' == 'true' "
    + "and '${minimall.xxl-job.enabled:false}' != 'true'")
public class OrderScheduleHandler {

    /** 分布式锁 TTL：扫描任务最长执行时长兜底，防止持有节点宕机后锁永不过期 */
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;

    public OrderScheduleHandler(OrderService orderService, StringRedisTemplate redisTemplate) {
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 每 1 分钟扫描一次超时未支付订单；SETNX 加锁失败说明其他实例正在执行，直接跳过。
     */
    @Scheduled(fixedDelayString = "${minimall.schedule.scan-expired-ms:60000}")
    public void scanExpiredOrders() {
        String lockKey = "order:schedule:scanExpiredOrders";
        if (!tryLock(lockKey)) {
            log.debug("scanExpiredOrders skipped, lock held by another instance");
            return;
        }
        try {
            int count = orderService.scanExpiredOrders();
            log.debug("scanExpiredOrders tick, processed={}", count);
        }
        finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 每 1 小时扫描一次超期未确认收货订单；SETNX 加锁失败说明其他实例正在执行，直接跳过。
     */
    @Scheduled(fixedDelayString = "${minimall.schedule.scan-auto-confirm-ms:3600000}")
    public void scanAutoConfirm() {
        String lockKey = "order:schedule:scanAutoConfirm";
        if (!tryLock(lockKey)) {
            log.debug("scanAutoConfirm skipped, lock held by another instance");
            return;
        }
        try {
            int count = orderService.scanAutoConfirm();
            log.debug("scanAutoConfirm tick, processed={}", count);
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
