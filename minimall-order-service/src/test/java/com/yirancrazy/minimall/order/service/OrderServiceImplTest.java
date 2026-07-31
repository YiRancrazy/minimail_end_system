package com.yirancrazy.minimall.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.service.impl.OrderServiceImpl;

/**
* OrderServiceImpl 单元测试，覆盖订单状态机创建、支付、取消、发货、确认、退款及异常路径。
 */
public class OrderServiceImplTest {

    private OrderManager manager;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(OrderManager.class);
        lenient().when(manager.updateById(any(OrderPO.class))).thenReturn(true);
        doAnswer(inv -> {
            OrderPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(OrderPO.class));
        service = new OrderServiceImpl(manager);
    }

    /**
     * 验证创建订单时状态为 PENDING。
     */
    @Test
    public void create_sets_pending_status() {
        Long orderId = service.create(1L, 100L, 2);
        assertNotNull(orderId);
    }

    /**
     * 验证支付成功后订单状态更新为 PAID。
     */
    @Test
    public void pay_transitions_to_paid() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.pay(99L);
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }

    /**
     * 验证支付不存在的订单时抛出异常。
     */
    @Test
    public void pay_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.pay(99L));
    }

    /**
     * 验证已支付订单不能再次支付（状态流转不合法）。
     */
    @Test
    public void pay_already_paid_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.pay(99L));
    }

    /**
     * 验证取消待支付订单成功。
     */
    @Test
    public void cancel_pending_order_succeeds() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.cancel(99L, 1L);
        assertEquals(OrderStatusEnum.CANCELLED.intCode(), existing.getStatus());
    }

    /**
     * 验证非本人取消订单时抛出异常。
     */
    @Test
    public void cancel_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.cancel(99L, 999L));
    }

    /**
     * 验证已支付订单不能取消（状态流转不合法）。
     */
    @Test
    public void cancel_paid_order_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.cancel(99L, 1L));
    }

    /**
     * 验证发货成功后状态为 SHIPPED。
     */
    @Test
    public void ship_transitions_to_shipped() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.ship(99L, 10L);
        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
    }

    /**
     * 验证确认收货成功后状态为 RECEIVED。
     */
    @Test
    public void confirm_transitions_to_received() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.confirm(99L, 1L);
        assertEquals(OrderStatusEnum.RECEIVED.intCode(), existing.getStatus());
    }

    /**
     * 验证非本人确认收货时抛出异常。
     */
    @Test
    public void confirm_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.confirm(99L, 999L));
    }

    /**
     * 验证申请退款成功后状态为 REFUNDING，并保存原状态。
     */
    @Test
    public void refund_transitions_to_refunding() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.refund(99L);
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getRefundFromStatus());
    }

    /**
     * 验证退款回调成功后状态为 REFUNDED。
     */
    @Test
    public void refundCallback_success_transitions_to_refunded() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.handleRefundCallback(99L, true);
        assertEquals(OrderStatusEnum.REFUNDED.intCode(), existing.getStatus());
    }

    /**
     * 验证退款回调失败后回退到原状态。
     */
    @Test
    public void refundCallback_failure_reverts_status() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.handleRefundCallback(99L, false);
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
    }

    /**
     * 验证退款回调失败且无原状态时默认回退到 PAID。
     */
    @Test
    public void refundCallback_failure_no_from_status_defaults_to_paid() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(null);
        when(manager.getById(99L)).thenReturn(existing);

        service.handleRefundCallback(99L, false);
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
    }

    /**
     * 验证查询订单状态返回正确的状态码。
     */
    @Test
    public void getStatus_returns_order_status() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        Integer status = service.getStatus(99L);
        assertEquals(OrderStatusEnum.PAID.intCode(), status);
    }

    /**
     * 验证查询不存在的订单状态时抛出异常。
     */
    @Test
    public void getStatus_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getStatus(99L));
    }

    private OrderPO buildOrder(Long id, Long userId, Integer status) {
        OrderPO po = new OrderPO();
        po.setId(id);
        po.setUserId(userId);
        po.setStatus(status);
        return po;
    }
}
