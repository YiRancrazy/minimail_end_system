package com.yirancrazy.minimall.pay.schedule;

import com.yirancrazy.minimall.pay.service.PayService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 以 Redis 分布式锁串行化支付补偿定时任务。
 * 任务分别补偿已支付未推进订单、待退款和未通知退款；依赖 `PayService` 与 `StringRedisTemplate`。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "minimall.schedule.enabled", havingValue = "true")
public class PayScheduleHandler {

    // redis 分布式锁的过期时间，单位为分钟
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT =
            RedisScript.of(
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else"
                            + " return 0 end",
                    Long.class);

    private final PayService payService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 注入补偿业务服务和 Redis 客户端。
     *
     * @param payService 执行三类补偿扫描的支付服务
     * @param redisTemplate 用于创建和释放定时任务分布式锁
     */
    public PayScheduleHandler(PayService payService, StringRedisTemplate redisTemplate) {
        this.payService = payService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 定时补偿已支付但订单尚未推进的流水。
     * 任务先获取专属 Redis 锁；锁被占用时跳过，执行后按 token 安全释放。
     */
    @Scheduled(
            fixedDelayString = "${minimall.schedule.pay.scan-paid-but-order-pending-ms}",
            initialDelayString = "${minimall.schedule.pay.scan-init-ms}")
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
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 定时补发已成功但尚未通知订单服务的退款。
     * 任务通过独立 Redis 锁防止多实例并发处理，并在 finally 中释放锁。
     */
    @Scheduled(
            fixedDelayString = "${minimall.schedule.pay.scan-unnotified-refunds-ms}",
            initialDelayString = "${minimall.schedule.pay.scan-init-ms}")
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
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 定时恢复待发起或处理中的退款。
     * 任务通过独立 Redis 锁串行化多实例执行，扫描异常仍会释放锁。
     */
    @Scheduled(
            fixedDelayString = "${minimall.schedule.pay.scan-pending-refunds-ms}",
            initialDelayString = "${minimall.schedule.pay.scan-init-ms}")
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
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * 使用随机 token 尝试获取带十分钟 TTL 的 Redis 锁。
     *
     * @param key 定时任务锁键
     * @return 获取成功时返回释放锁所需 token，否则返回 null
     */
    private String tryLock(String key) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL);
        return Boolean.TRUE.equals(locked) ? token : null;
    }

    /**
     * 使用 Lua 脚本仅在 token 匹配时删除分布式锁。
     * 释放失败只记录告警，避免影响定时任务主流程。
     *
     * @param key 定时任务锁键
     * @param token 加锁时生成的随机 token
     */
    private void releaseLock(String key, String token) {
        try {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(key), token);
        } catch (Exception e) {
            log.warn("释放定时任务锁失败，key={}", key, e);
        }
    }
}
