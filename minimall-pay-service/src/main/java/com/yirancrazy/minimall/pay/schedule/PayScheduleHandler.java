package com.yirancrazy.minimall.pay.schedule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    private final PayService payService;

    /**
     * 构造 PayScheduleHandler 实例。
     * @param payService payService 参数
     */
    public PayScheduleHandler(PayService payService) {
        this.payService = payService;
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPaidButOrderPending() {
        int count = payService.scanPaidButOrderPending();
        log.debug("scanPaidButOrderPending 执行，processed={}", count);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanUnnotifiedRefunds() {
        int count = payService.scanUnnotifiedRefunds();
        log.debug("scanUnnotifiedRefunds 执行，processed={}", count);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPendingRefunds() {
        int count = payService.scanPendingRefunds();
        log.debug("scanPendingRefunds 执行，processed={}", count);
    }
}
