package com.yirancrazy.minimall.notify.listener;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.sse.SseHub;

/**
* OrderPaidListener 单元测试，验证事件触发后消息持久化与 SSE 推送的协作逻辑。
 */
public class OrderPaidListenerTest {

    private NotifyManager manager;
    private SseHub sseHub;
    private NotifyService notifyService;
    private OrderPaidListener listener;

    @BeforeEach
    void setUp() {
        manager = mock(NotifyManager.class);
        sseHub = mock(SseHub.class);
        lenient().when(manager.list((Wrapper<NotifyMessagePO>) any())).thenReturn(Collections.emptyList());
        doAnswer(inv -> {
            NotifyMessagePO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(NotifyMessagePO.class));
        notifyService = new NotifyService(manager);
        listener = new OrderPaidListener(notifyService, sseHub);
    }

    /**
     * 订单已支付事件触发后，应保存一条 notify 消息且用户 ID 正确，并向对应用户推送一次 SSE。
     */
    @Test
    public void on_order_paid_saves_message_and_pushes_sse() {
        OrderPaidDTO ev = new OrderPaidDTO(99L, 7L, new BigDecimal("100.00"), "2026-07-29T10:00:00");
        listener.onOrderPaid(ev);

        ArgumentCaptor<NotifyMessagePO> cap = ArgumentCaptor.forClass(NotifyMessagePO.class);
        verify(manager, times(1)).save(cap.capture());
        assertEquals(7L, cap.getValue().getUserId());
        assertNotNull(cap.getValue().getTitle());
        assertNotNull(cap.getValue().getContent());

        verify(sseHub, times(1)).send(eq(7L), anyString());
    }
}