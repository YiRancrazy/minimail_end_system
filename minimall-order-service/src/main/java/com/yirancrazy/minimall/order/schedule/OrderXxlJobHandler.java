package com.yirancrazy.minimall.order.schedule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: XXL-Job 任务处理器，仅在 minimall.xxl-job.enabled=true 时启用。复用 OrderService 既有定时逻辑，与 Spring @Scheduled 双轨制。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Slf4j
@Component
@ConditionalOnProperty(name = "minimall.xxl-job.enabled", havingValue = "true")
public class OrderXxlJobHandler {

    private final OrderService orderService;

    public OrderXxlJobHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 扫描超时未支付订单，由 XXL-Job 调度。
     */
    @XxlJob("scanExpiredOrders")
    public void scanExpiredOrders() {
        int count = orderService.scanExpiredOrders();
        log.info("xxl-job scanExpiredOrders, processed={}", count);
    }

    /**
     * 扫描发货后超期未确认收货订单，由 XXL-Job 调度。
     */
    @XxlJob("scanAutoConfirm")
    public void scanAutoConfirm() {
        int count = orderService.scanAutoConfirm();
        log.info("xxl-job scanAutoConfirm, processed={}", count);
    }
}
