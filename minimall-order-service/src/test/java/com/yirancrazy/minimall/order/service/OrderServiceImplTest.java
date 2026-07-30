package com.yirancrazy.minimall.order.service;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.NotifyFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.EventBus;
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

/**
* OrderServiceImpl 单元测试，覆盖订单创建、库存锁定失败、支付成功及订单缺失异常路径。
 */
public class OrderServiceImplTest {

    private OrderManager manager;
    private StockFeignClient stockFeign;
    private PayFeignClient payFeign;
    private NotifyFeignClient notifyFeign;
    private EventBus eventBus;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(OrderManager.class);
        stockFeign = mock(StockFeignClient.class);
        payFeign = mock(PayFeignClient.class);
        notifyFeign = mock(NotifyFeignClient.class);
        eventBus = mock(EventBus.class);
        lenient().when(manager.updateById(any(OrderPO.class))).thenReturn(true);
        lenient().when(notifyFeign.push(any(NotifyEventDTO.class))).thenReturn(true);
        doAnswer(inv -> {
            OrderPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(OrderPO.class));
        service = new OrderServiceImpl(manager, stockFeign, payFeign, notifyFeign, eventBus);
    }

    /**
     * 验证创建订单时依次锁定库存、创建支付流水并返回订单标识。
     */
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
        assertEquals(orderId.toString(), payCap.getValue().getOrderNo());
    }

    /**
     * 验证库存锁定失败时创建订单抛出业务异常且不继续支付流程。
     */
    @Test
    public void create_falls_back_when_stock_unavailable() {
        when(stockFeign.reserve(any(StockReserveDTO.class))).thenReturn(false);
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }

    /**
     * 验证支付成功后订单状态更新为已支付，并推送通知及发布支付事件。
     */
    @Test
    public void pay_marks_paid_and_publishes_event() {
        OrderPO existing = new OrderPO();
        existing.setId(99L);
        existing.setUserId(1L);
        existing.setPayId(7777L);
        existing.setStatus("PENDING_PAY");
        when(manager.getById(99L)).thenReturn(existing);
        when(payFeign.callback(7777L)).thenReturn(true);
        when(notifyFeign.push(any(NotifyEventDTO.class))).thenReturn(true);

        boolean ok = service.pay(99L);
        assertEquals(true, ok);
        assertEquals("PAID", existing.getStatus());

        verify(notifyFeign, times(1)).push(any(NotifyEventDTO.class));
        verify(eventBus, times(1)).publish(any(OrderPaidDTO.class));
        Mockito.verify(payFeign).callback(7777L);
    }

    /**
     * 验证支付不存在的订单时抛出订单不存在业务异常。
     */
    @Test
    public void pay_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.pay(99L));
    }

    /**
     * 验证支付回调失败时抛出支付失败业务异常。
     */
    @Test
    public void pay_callback_fail_throws() {
        OrderPO existing = new OrderPO();
        existing.setId(99L);
        existing.setUserId(1L);
        existing.setPayId(7777L);
        existing.setStatus("PENDING_PAY");
        when(manager.getById(99L)).thenReturn(existing);
        when(payFeign.callback(7777L)).thenReturn(false);

        assertThrows(BizException.class, () -> service.pay(99L));
    }

    /**
     * 验证支付已完成订单时返回 false 且不更新状态。
     */
    @Test
    public void pay_already_paid_returns_false() {
        OrderPO existing = new OrderPO();
        existing.setId(99L);
        existing.setUserId(1L);
        existing.setPayId(7777L);
        existing.setStatus("PAID");
        when(manager.getById(99L)).thenReturn(existing);

        boolean ok = service.pay(99L);
        assertEquals(false, ok);
        assertEquals("PAID", existing.getStatus());
    }

    /**
     * 验证查询存在的订单状态返回正确状态值。
     */
    @Test
    public void status_returns_order_status() {
        OrderPO existing = new OrderPO();
        existing.setId(99L);
        existing.setStatus("PAID");
        when(manager.getById(99L)).thenReturn(existing);

        String status = service.status(99L);
        assertEquals("PAID", status);
    }

    /**
     * 验证查询不存在的订单状态返回 UNKNOWN。
     */
    @Test
    public void status_missing_returns_unknown() {
        when(manager.getById(99L)).thenReturn(null);

        String status = service.status(99L);
        assertEquals("UNKNOWN", status);
    }
}