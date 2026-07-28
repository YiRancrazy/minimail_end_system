package com.yirancrazy.minimall.order.service;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.LocalEventBus;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderServiceImplTest {

    private OrderManager manager;
    private StockFeignClient stockFeign;
    private PayFeignClient payFeign;
    private LocalEventBus eventBus;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(OrderManager.class);
        stockFeign = mock(StockFeignClient.class);
        payFeign = mock(PayFeignClient.class);
        eventBus = mock(LocalEventBus.class);
        lenient().when(manager.updateById(any(OrderPO.class))).thenReturn(true);
        doAnswer(inv -> {
            OrderPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(OrderPO.class));
        service = new OrderServiceImpl(manager, stockFeign, payFeign, eventBus);
    }

    @Test
    public void create_chain_calls_stock_and_pay() {
        when(stockFeign.reserve(any(StockReserveDTO.class))).thenReturn(true);
        when(payFeign.create(any(PayCreateDTO.class))).thenReturn(7777L);

        Long orderId = service.create(1L, 100L, 2);
        assertNotNull(orderId);

        ArgumentCaptor<StockReserveDTO> stockCap = ArgumentCaptor.forClass(StockReserveDTO.class);
        verify(stockFeign).reserve(stockCap.capture());
        assertEquals(100L, stockCap.getValue().getSkuId());
        assertEquals(2, stockCap.getValue().getQuantity());

        ArgumentCaptor<PayCreateDTO> payCap = ArgumentCaptor.forClass(PayCreateDTO.class);
        verify(payFeign).create(payCap.capture());
        assertEquals(orderId, payCap.getValue().getOrderId());
    }

    @Test
    public void create_falls_back_when_stock_unavailable() {
        when(stockFeign.reserve(any(StockReserveDTO.class))).thenReturn(false);
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }

    @Test
    public void pay_marks_paid_and_publishes_event() {
        OrderPO existing = new OrderPO();
        existing.setId(99L);
        existing.setUserId(1L);
        existing.setPayId(7777L);
        existing.setStatus("PENDING_PAY");
        when(manager.getById(99L)).thenReturn(existing);
        when(payFeign.callback(7777L)).thenReturn(true);

        boolean ok = service.pay(99L);
        assertEquals(true, ok);
        assertEquals("PAID", existing.getStatus());

        ArgumentCaptor<Object> cap = ArgumentCaptor.forClass(Object.class);
        verify(eventBus, times(1)).publish(cap.capture());
        assertNotNull(cap.getValue());
        assertEquals(99L, ((OrderPaidDTO) cap.getValue()).getOrderId());
        Mockito.verify(payFeign).callback(7777L);
    }

    @Test
    public void pay_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.pay(99L));
    }
}