package com.yirancrazy.minimall.notify.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaid监听器，监听OrderPaid相关事件并落库站内信
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
@Slf4j
@Component
public class OrderPaidListener {

    private final NotifyService notifyService;

    public OrderPaidListener(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * 订阅 in-process LocalEventBus（订单服务在同一进程 publish 时才会触发）。
     * 跨服务部署时由 MQ 替代（Iter-3）。SSE 实时推送由 push 内部统一负责。
     */
    @EventListener
    public void onOrderPaid(OrderPaidDTO event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        String title = "订单支付成功";
        String content = "订单 " + event.getOrderId() + " 已支付，金额 " + event.getAmount();
        notifyService.push(event.getUserId(), title, content);
        log.info("notified user {} of order {}", event.getUserId(), event.getOrderId());
    }
}
