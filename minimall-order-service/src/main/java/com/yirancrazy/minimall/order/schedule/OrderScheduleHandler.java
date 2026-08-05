package com.yirancrazy.minimall.order.schedule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单定时任务调度器，触发超时取消与自动确认收货扫描。ponytail: 用 Spring @Scheduled 替代 XXL-Job，等部署 XXL-Job admin 再切换。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Slf4j
@Component
@ConditionalOnProperty(name = "minimall.schedule.enabled", havingValue = "true")
public class OrderScheduleHandler {

    private final OrderService orderService;

    public OrderScheduleHandler(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 每 1 分钟扫描一次超时未支付订单。
     */
    @Scheduled(fixedDelayString = "${minimall.schedule.scan-expired-ms:60000}")
    public void scanExpiredOrders() {
        int count = orderService.scanExpiredOrders();
        log.debug("scanExpiredOrders tick, processed={}", count);
    }

    /**
     * 每 1 小时扫描一次超期未确认收货订单。
     */
    @Scheduled(fixedDelayString = "${minimall.schedule.scan-auto-confirm-ms:3600000}")
    public void scanAutoConfirm() {
        int count = orderService.scanAutoConfirm();
        log.debug("scanAutoConfirm tick, processed={}", count);
    }
}
