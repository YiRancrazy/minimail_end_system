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
 * @Description: OrderScheduleHandler 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
// XXL-Job 启用时禁用本套定时任务，双轨互斥防止同一扫描被两套调度器重复触发
@ConditionalOnExpression("'${minimall.schedule.enabled:false}' == 'true' "
    + "and '${minimall.xxl-job.enabled:false}' != 'true'")
public class OrderScheduleHandler {


    private static final Duration LOCK_TTL = Duration.ofMinutes(10);


    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = RedisScript.of(
        "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
        Long.class);

    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造 OrderScheduleHandler 实例。
     * @param orderService orderService 参数
     * @param redisTemplate redisTemplate 参数
     */
    public OrderScheduleHandler(OrderService orderService, StringRedisTemplate redisTemplate) {
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
    }


    /**
     * 扫描并处理待办数据。
     */
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
     * 扫描并处理待办数据。
     */
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
     * 扫描并处理待办数据。
     */
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
     * 处理try锁定。
     * @param key 键
     * @return 处理结果
     */
    private String tryLock(String key) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, token, LOCK_TTL);
        return Boolean.TRUE.equals(locked) ? token : null;
    }


    /**
     * 执行锁定操作。
     * @param key 键
     * @param token Token
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
