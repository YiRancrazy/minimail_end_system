package com.yirancrazy.minimall.notify.listener;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.sse.SseHub;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
* 订单已支付事件监听器，订阅 LocalEventBus 后保存通知并向在线用户 SSE 推送。
 */
@Slf4j
@Component
public class OrderPaidListener {

    private final NotifyService notifyService;
    private final SseHub sseHub;

    public OrderPaidListener(NotifyService notifyService, SseHub sseHub) {
        this.notifyService = notifyService;
        this.sseHub = sseHub;
    }

    /**
     * 订阅 in-process LocalEventBus（订单服务在同一进程 publish 时才会触发）。
     * 跨服务部署时由 MQ 替代（Iter-3）。
     */
    @EventListener
    public void onOrderPaid(OrderPaidDTO event) {
        if (event == null || event.getOrderId() == null) return;
        String title = "订单支付成功";
        String content = "订单 " + event.getOrderId() + " 已支付，金额 " + event.getAmount();
        NotifyEventDTO dto = new NotifyEventDTO(event.getUserId(), title, content);
        notifyService.push(dto.getUserId(), dto.getTitle(), dto.getContent());
        sseHub.send(dto.getUserId(), dto.getTitle() + ": " + dto.getContent());
        log.info("notified user {} of order {}", dto.getUserId(), event.getOrderId());
    }
}