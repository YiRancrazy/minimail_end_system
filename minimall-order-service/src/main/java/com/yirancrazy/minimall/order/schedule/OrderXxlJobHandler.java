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
 * @Description: OrderXxlJobHandler 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
@ConditionalOnProperty(name = "minimall.xxl-job.enabled", havingValue = "true")
public class OrderXxlJobHandler {


    private static final Duration LOCK_TTL = Duration.ofSeconds(60);

    private final OrderService orderService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造 OrderXxlJobHandler 实例。
     * @param orderService orderService 参数
     * @param redisTemplate redisTemplate 参数
     */
    public OrderXxlJobHandler(OrderService orderService, StringRedisTemplate redisTemplate) {
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
    }


    /**
     * 扫描并处理待办数据。
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
        }
        finally {
            redisTemplate.delete(lockKey);
        }
    }


    /**
     * 扫描并处理待办数据。
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
        }
        finally {
            redisTemplate.delete(lockKey);
        }
    }


    /**
     * 扫描并处理待办数据。
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
        }
        finally {
            redisTemplate.delete(lockKey);
        }
    }


    /**
     * 处理try锁定。
     * @param key 键
     * @return 是否满足条件
     */
    private boolean tryLock(String key) {
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL);
        return Boolean.TRUE.equals(locked);
    }
}
