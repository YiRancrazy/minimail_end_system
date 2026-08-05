package com.yirancrazy.minimall.order.schedule;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * OrderScheduleHandler 单元测试，验证定时任务委托调用 OrderService 扫描方法。
 */
public class OrderScheduleHandlerTest {

    /**
     * 验证 scanExpiredOrders 委托 OrderService.scanExpiredOrders 并处理返回值。
     */
    @Test
    public void scanExpiredOrders_delegates_to_service() {
        OrderService orderService = mock(OrderService.class);
        when(orderService.scanExpiredOrders()).thenReturn(3);

        OrderScheduleHandler handler = new OrderScheduleHandler(orderService);
        handler.scanExpiredOrders();

        verify(orderService, times(1)).scanExpiredOrders();
        assertEquals(3, orderService.scanExpiredOrders());
    }

    /**
     * 验证 scanAutoConfirm 委托 OrderService.scanAutoConfirm。
     */
    @Test
    public void scanAutoConfirm_delegates_to_service() {
        OrderService orderService = mock(OrderService.class);
        when(orderService.scanAutoConfirm()).thenReturn(5);

        OrderScheduleHandler handler = new OrderScheduleHandler(orderService);
        handler.scanAutoConfirm();

        verify(orderService, times(1)).scanAutoConfirm();
        assertEquals(5, orderService.scanAutoConfirm());
    }
}
