package com.yirancrazy.minimall.order.schedule;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单定时任务调度器，触发超时取消与自动确认收货扫描。ponytail: 用 Spring @Scheduled 替代 XXL-Job，等部署 XXL-Job admin 再切换。
 * @Version: 1.2
 * @DateTime: 2026/08/05
 **/
@Slf4j
@Component
// XXL-Job 启用时禁用本套定时任务，双轨互斥防止同一扫描被两套调度器重复触发
@ConditionalOnExpression("'${minimall.schedule.enabled:false}' == 'true' "
    + "and '${minimall.xxl-job.enabled:false}' != 'true'")
public class OrderScheduleHandler {

    /** 分布式锁 TTL：分批扫描可能耗时数分钟，TTL 需覆盖单轮扫描并为宕机兜底 */
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    /** 释放锁时比较持有者标识，避免扫描超时后误删其他实例重新获取的锁 */
    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = RedisScript.of(
        "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
        Long.class);

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
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            log.debug("scanExpiredOrders 已跳过，锁被其他实例持有");
            return;
        }
        try {
            int count = orderService.scanExpiredOrders();
            log.debug("scanExpiredOrders 执行完成，processed={}", count);
        }
        finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 每 1 小时扫描一次超期未确认收货订单；SETNX 加锁失败说明其他实例正在执行，直接跳过。
     */
    @Scheduled(fixedDelayString = "${minimall.schedule.scan-auto-confirm-ms:3600000}")
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
        }
        finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 每 1 分钟扫描一次已取消但库存仍待释放的订单，供失败后补偿重试。
     */
    @Scheduled(fixedDelayString = "${minimall.schedule.scan-stock-release-ms:60000}")
    public void scanStockReleasePending() {
        String lockKey = "order:schedule:scanStockReleasePending";
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            return;
        }
        try {
            int count = orderService.scanStockReleasePending();
            log.debug("scanStockReleasePending 执行完成，processed={}", count);
        }
        finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * Redis SETNX 加锁，写入随机持有者标识，防止多实例并发执行同一扫描任务。
     * @param key 锁键
     * @return 持有者标识；获取失败返回 null
     */
    private String tryLock(String key) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL);
        return Boolean.TRUE.equals(locked) ? token : null;
    }

    /**
     * 原子比较持有者标识后删除锁：标识不匹配说明锁已过期并被其他实例持有，不删除。
     * @param key 锁键
     * @param token 本实例持有者标识
     */
    private void releaseLock(String key, String token) {
        try {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(key), token);
        }
        catch (Exception e) {
            log.warn("释放定时任务锁失败，key={}", key, e);
        }
    }
}
