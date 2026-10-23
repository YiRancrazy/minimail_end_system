package com.yirancrazy.minimall.pay.schedule;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.pay.service.PayService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayScheduleHandler 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
@ConditionalOnProperty(name = "minimall.schedule.enabled", havingValue = "true")
public class PayScheduleHandler {

    // redis 分布式锁的过期时间，单位为分钟
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = RedisScript.of(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
            Long.class);

    private final PayService payService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造 PayScheduleHandler 实例。
     * @param payService payService 参数
     * @param redisTemplate Redis 客户端
     */
    public PayScheduleHandler(PayService payService, StringRedisTemplate redisTemplate) {
        this.payService = payService;
        this.redisTemplate = redisTemplate;
    }


    /**
     * 扫描已支付但订单未完成的数据。
     */
    @Scheduled(
        fixedDelayString = "${minimall.schedule.pay.scan-paid-but-order-pending-ms}",
        initialDelayString = "${minimall.schedule.pay.scan-init-ms}"
    )
    public void scanPaidButOrderPending() {
        String lockKey = "pay:schedule:scanPaidButOrderPending";
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            log.warn("scanPaidButOrderPending 锁定失败，key={}", lockKey);
            return;
        }

        try {
            int count = payService.scanPaidButOrderPending();
            log.debug("scanPaidButOrderPending 执行，processed={}", count);
        }
        finally {
            releaseLock(lockKey, lockToken);
        }
    }


    /**
     * 扫描未通知的退款数据。
     */
    @Scheduled(
        fixedDelayString = "${minimall.schedule.pay.scan-unnotified-refunds-ms}",
        initialDelayString = "${minimall.schedule.pay.scan-init-ms}"
    )
    public void scanUnnotifiedRefunds() {
        String lockKey = "pay:schedule:scanUnnotifiedRefunds";
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            log.warn("scanUnnotifiedRefunds 锁定失败，key={}", lockKey);
            return;
        }
        try {
            int count = payService.scanUnnotifiedRefunds();
            log.debug("scanUnnotifiedRefunds 执行，processed={}", count);
        }
        finally {
            releaseLock(lockKey, lockToken);
        }
    }


    /**
     * 扫描并处理待退款数据。
     */
    @Scheduled(
        fixedDelayString = "${minimall.schedule.pay.scan-pending-refunds-ms}",
        initialDelayString = "${minimall.schedule.pay.scan-init-ms}"
    )
    public void scanPendingRefunds() {
        String lockKey = "pay:schedule:scanPendingRefunds";
        String lockToken = tryLock(lockKey);
        if (lockToken == null) {
            log.warn("scanPendingRefunds 锁定失败，key={}", lockKey);
            return;
        }
        try {
            int count = payService.scanPendingRefunds();
            log.debug("scanPendingRefunds 执行，processed={}", count);
        }
        finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 尝试获取定时任务分布式锁。
     * @param key 锁键
     * @return 加锁成功时返回锁 token，否则返回 null
     */
    private String tryLock(String key) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL);
        return Boolean.TRUE.equals(locked) ? token : null;
    }


    /**
     * 使用加锁 token 安全释放分布式锁。
     * @param key 锁键
     * @param token 加锁时生成的 token
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
