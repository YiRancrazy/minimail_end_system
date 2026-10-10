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
        log.debug("scanPaidButOrderPending 执行，processed={}", count);
    }

    /**
     * 每 30 秒扫描一次退款成功但订单未收到成功回调的退款单，重试通知订单服务。
     */
    @Scheduled(fixedDelayString = "${minimall.schedule.scan-callback-ms:30000}")
    public void scanUnnotifiedRefunds() {
        int count = payService.scanUnnotifiedRefunds();
        log.debug("scanUnnotifiedRefunds 执行，processed={}", count);
    }

    /**
     * 每 30 秒扫描一次 PENDING 退款，查询渠道并恢复中断的退款流程。
     */
    @Scheduled(fixedDelayString = "${minimall.schedule.scan-refund-ms:30000}")
    public void scanPendingRefunds() {
        int count = payService.scanPendingRefunds();
        log.debug("scanPendingRefunds 执行，processed={}", count);
    }
}
