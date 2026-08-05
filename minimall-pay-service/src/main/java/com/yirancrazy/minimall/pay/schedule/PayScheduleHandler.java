package com.yirancrazy.minimall.pay.schedule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.pay.service.PayService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付定时任务调度器，触发支付成功但订单未推进的 RPC 兜底扫描。ponytail: 用 Spring @Scheduled 替代 XXL-Job，等部署 XXL-Job admin 再切换。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Slf4j
@Component
@ConditionalOnProperty(name = "minimall.schedule.enabled", havingValue = "true")
public class PayScheduleHandler {

    private final PayService payService;

    public PayScheduleHandler(PayService payService) {
        this.payService = payService;
    }

    /**
     * 每 30 秒扫描一次支付成功但订单未推进的流水，作为回调丢失时的 RPC 兜底。
     */
    @Scheduled(fixedDelayString = "${minimall.schedule.scan-callback-ms:30000}")
    public void scanPaidButOrderPending() {
        int count = payService.scanPaidButOrderPending();
        log.debug("scanPaidButOrderPending tick, processed={}", count);
    }
}
