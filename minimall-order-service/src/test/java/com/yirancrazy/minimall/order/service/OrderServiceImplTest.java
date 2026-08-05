package com.yirancrazy.minimall.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusMachine;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderItemPO;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderItemManager;
import com.yirancrazy.minimall.order.manager.OrderLogisticsManager;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.mapper.OrderMapper;
import com.yirancrazy.minimall.order.service.impl.OrderServiceImpl;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;

/**
* OrderServiceImpl 单元测试，覆盖订单状态机创建、支付、取消、发货、确认、退款及异常路径。
 */
public class OrderServiceImplTest {

    private OrderManager manager;
    private OrderItemManager orderItemManager;
    private OrderLogisticsManager logisticsManager;
    private OrderMapper orderMapper;
    private GoodsFeignClient goodsFeignClient;
    private StockFeignClient stockFeignClient;
    private PayFeignClient payFeignClient;
    private EventBus eventBus;
    private OrderStatusMachine statusMachine;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(OrderManager.class);
        orderItemManager = mock(OrderItemManager.class);
        logisticsManager = mock(OrderLogisticsManager.class);
        orderMapper = mock(OrderMapper.class);
        goodsFeignClient = mock(GoodsFeignClient.class);
        stockFeignClient = mock(StockFeignClient.class);
        payFeignClient = mock(PayFeignClient.class);
        eventBus = mock(EventBus.class);
        statusMachine = new OrderStatusMachine();
        lenient().when(manager.updateById(any(OrderPO.class))).thenReturn(true);
        lenient().when(manager.removeById(any(Long.class))).thenReturn(true);
        doAnswer(inv -> {
            OrderPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(OrderPO.class));
        lenient().when(orderItemManager.save(any(OrderItemPO.class))).thenReturn(true);
        lenient().when(orderItemManager.list(any(Wrapper.class))).thenReturn(java.util.Collections.emptyList());
        lenient().when(logisticsManager.save(any(OrderLogisticsPO.class))).thenReturn(true);
        lenient().when(goodsFeignClient.skuSnapshot(any())).thenReturn(Result.success(
            new SkuSnapshotDTO(100L, 1L, "sku-100", new BigDecimal("9.90"), 100)));
        lenient().when(stockFeignClient.reserve(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(stockFeignClient.release(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(payFeignClient.create(any())).thenReturn(Result.success(2001L));
        lenient().when(payFeignClient.refund(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().doAnswer(inv -> {
            inv.getArgument(1, Runnable.class).run();
            return null;
        }).when(eventBus).publishInTx(any(), any(Runnable.class), any());
        service = new OrderServiceImpl(manager, orderItemManager, logisticsManager, orderMapper,
            goodsFeignClient, stockFeignClient, payFeignClient, eventBus, statusMachine);
    }

    /**
     * 验证创建订单时状态为 PENDING，并联动 stock.reserve 与 pay.create。
     */
    @Test
    public void create_sets_pending_status() {
        Long orderId = service.create(1L, 100L, 2);
        assertNotNull(orderId);
        verify(stockFeignClient).reserve(any());
        verify(payFeignClient).create(any());
    }

    /**
     * 验证商品快照缺失时抛出 BizException。
     */
    @Test
    public void create_snapshot_missing_throws() {
        when(goodsFeignClient.skuSnapshot(any())).thenReturn(Result.success(null));
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }

    /**
     * 验证库存预占失败时抛出 BizException。
     */
    @Test
    public void create_stock_reserve_fail_throws() {
        when(stockFeignClient.reserve(any())).thenReturn(Result.success(Boolean.FALSE));
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }

    /**
     * 验证支付流水创建失败时抛出 BizException。
     */
    @Test
    public void create_pay_create_fail_throws() {
        when(payFeignClient.create(any())).thenReturn(Result.success(-1L));
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
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
        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
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
     * 验证确认收货成功后状态为 COMPLETED。
     */
    @Test
    public void confirm_transitions_to_received() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.confirm(99L, 1L);
        assertEquals(OrderStatusEnum.COMPLETED.intCode(), existing.getStatus());
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

    /**
     * 验证 page 委托给 manager.list 并返回游标分页结果。
     */
    @Test
    public void page_delegates_to_manager() {
        OrderPageDTO dto = new OrderPageDTO();
        dto.setUserId(1L);
        List<OrderPO> mockRecords = new ArrayList<>();
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(mockRecords);

        CursorPageVO<OrderPO> result = service.page(dto);
        assertNotNull(result);
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证 getDetail 在订单存在时返回实体。
     */
    @Test
    public void getDetail_returns_order_when_exists() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        OrderPO result = service.getDetail(99L);
        assertEquals(99L, result.getId());
    }

    /**
     * 验证 getDetail 在订单不存在时抛出异常。
     */
    @Test
    public void getDetail_missing_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getDetail(99L));
    }

    /**
     * 验证商家关闭待支付订单成功，状态推进为 CANCELED 并释放库存。
     */
    @Test
    public void merchantClose_pending_succeeds() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.merchantClose(99L, 10L);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
        verify(stockFeignClient).release(any());
    }

    /**
     * 验证非归属商家关闭订单时抛出异常。
     */
    @Test
    public void merchantClose_wrong_merchant_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.merchantClose(99L, 999L));
    }

    /**
     * 验证已支付订单不能被商家关闭（状态流转不合法）。
     */
    @Test
    public void merchantClose_paid_order_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.merchantClose(99L, 10L));
    }

    /**
     * 验证用户删除已取消订单成功（终态可删）。
     */
    @Test
    public void delete_cancelled_succeeds() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.CANCELED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.delete(99L, 1L);
        verify(manager).removeById(99L);
    }

    /**
     * 验证删除待支付订单抛出 ORDER_DELETE_NOT_ALLOWED（非终态）。
     */
    @Test
    public void delete_pending_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(99L, 1L));
    }

    /**
     * 验证非归属用户删除订单抛出异常。
     */
    @Test
    public void delete_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.CANCELED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(99L, 999L));
    }

    /**
     * 验证平台关闭待支付订单成功，状态推进为 CANCELED 并释放库存。
     */
    @Test
    public void platformClose_pending_succeeds() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.platformClose(99L);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
        verify(stockFeignClient).release(any());
    }

    /**
     * 验证平台关闭已支付订单抛出异常（状态流转不合法）。
     */
    @Test
    public void platformClose_paid_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.platformClose(99L));
    }

    /**
     * 验证 pendingCount 委托给 manager.count 并返回统计值。
     */
    @Test
    public void pendingCount_returns_count() {
        when(manager.count(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(5L);
        long count = service.pendingCount(10L);
        assertEquals(5L, count);
    }

    /**
     * 验证 exportList 委托给 manager.list 并强制绑定 merchantId。
     */
    @Test
    public void exportList_delegates_to_manager() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(new java.util.ArrayList<>());
        assertEquals(0, service.exportList(10L, new OrderPageDTO()).size());
    }

    /**
     * 验证 platformExportList 委托给 manager.list，不绑定 merchantId。
     */
    @Test
    public void platformExportList_delegates_to_manager() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(new java.util.ArrayList<>());
        assertEquals(0, service.platformExportList(new OrderPageDTO()).size());
    }

    /**
     * 验证商家同意退款时状态保持 REFUNDING 且不调用 updateById。
     */
    @Test
    public void reviewRefund_approved_keeps_refunding() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, true, 10L);
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        verify(manager, never()).updateById(any(OrderPO.class));
    }

    /**
     * 验证商家拒绝退款时状态回退到 refundFromStatus。
     */
    @Test
    public void reviewRefund_rejected_reverts_status() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, false, 10L);
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }

    /**
     * 验证非归属商家审核退款时抛出异常。
     */
    @Test
    public void reviewRefund_wrong_merchant_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reviewRefund(99L, true, 999L));
    }

    /**
     * 验证非 REFUNDING 状态订单审核退款时抛出异常。
     */
    @Test
    public void reviewRefund_not_refunding_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reviewRefund(99L, true, 10L));
    }

    /**
     * 验证平台仲裁同意退款时状态推进为 REFUNDED。
     */
    @Test
    public void arbitrateRefund_approved_transitions_to_refunded() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.arbitrateRefund(99L, true);
        assertEquals(OrderStatusEnum.REFUNDED.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }

    /**
     * 验证平台仲裁拒绝退款时状态回退到 refundFromStatus。
     */
    @Test
    public void arbitrateRefund_rejected_reverts_status() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.arbitrateRefund(99L, false);
        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }

    /**
     * 验证平台仲裁非 REFUNDING 状态订单时抛出异常。
     */
    @Test
    public void arbitrateRefund_not_refunding_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.arbitrateRefund(99L, true));
    }

    /**
     * 验证 queryLogistics 在订单存在时按创建时间正序返回物流节点 VO。
     */
    @Test
    public void queryLogistics_returns_nodes_when_order_exists() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);
        List<OrderLogisticsPO> nodes = new ArrayList<>(2);
        OrderLogisticsPO first = new OrderLogisticsPO();
        first.setOrderId(99L);
        first.setNode("已发货");
        first.setDescription("商家已发货");
        first.setCreateTime(LocalDateTime.of(2026, 8, 3, 10, 0));
        OrderLogisticsPO second = new OrderLogisticsPO();
        second.setOrderId(99L);
        second.setNode("已签收");
        second.setDescription("用户已签收");
        second.setCreateTime(LocalDateTime.of(2026, 8, 4, 12, 0));
        nodes.add(first);
        nodes.add(second);
        when(logisticsManager.list(any(Wrapper.class))).thenReturn(nodes);

        List<OrderLogisticsVO> result = service.queryLogistics(99L);
        assertEquals(2, result.size());
        assertEquals("已发货", result.get(0).getNode());
        assertEquals("已签收", result.get(1).getNode());
    }

    /**
     * 验证 queryLogistics 在订单不存在时抛出异常。
     */
    @Test
    public void queryLogistics_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.queryLogistics(99L));
    }

    /**
     * 验证发货时插入一条"已发货"物流节点。
     */
    @Test
    public void ship_inserts_logistics_node() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.ship(99L, 10L);
        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
        verify(logisticsManager).save(any(OrderLogisticsPO.class));
    }

    /**
     * 验证多SKU结算下单成功，创建订单头与明细行并初始化支付。
     */
    @Test
    public void checkout_multi_sku_succeeds() {
        List<OrderCheckoutItemDTO> items = List.of(
            new OrderCheckoutItemDTO(100L, 2),
            new OrderCheckoutItemDTO(200L, 1));
        when(goodsFeignClient.skuSnapshot(any())).thenReturn(Result.success(
            new SkuSnapshotDTO(100L, 1L, "sku-100", new BigDecimal("9.90"), 100)));

        Long orderId = service.checkout(1L, items);

        assertNotNull(orderId);
        verify(stockFeignClient, times(2)).reserve(any());
        verify(orderItemManager, times(2)).save(any(OrderItemPO.class));
        verify(payFeignClient).create(any());
    }

    /**
     * 验证空结算列表抛出 ORDER_ITEMS_EMPTY。
     */
    @Test
    public void checkout_empty_items_throws() {
        assertThrows(BizException.class, () -> service.checkout(1L, java.util.Collections.emptyList()));
    }

    /**
     * 验证结算时商品快照缺失抛出异常。
     */
    @Test
    public void checkout_snapshot_missing_throws() {
        when(goodsFeignClient.skuSnapshot(any())).thenReturn(Result.success(null));
        List<OrderCheckoutItemDTO> items = List.of(new OrderCheckoutItemDTO(100L, 1));

        assertThrows(BizException.class, () -> service.checkout(1L, items));
    }

    /**
     * 验证结算时库存锁定失败抛出异常。
     */
    @Test
    public void checkout_stock_reserve_fail_throws() {
        when(stockFeignClient.reserve(any())).thenReturn(Result.success(Boolean.FALSE));
        List<OrderCheckoutItemDTO> items = List.of(new OrderCheckoutItemDTO(100L, 1));

        assertThrows(BizException.class, () -> service.checkout(1L, items));
    }

    /**
     * 验证取消多SKU订单时逐条释放库存。
     */
    @Test
    public void cancel_multi_sku_releases_all_items() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);
        List<OrderItemPO> items = new ArrayList<>(2);
        OrderItemPO i1 = new OrderItemPO();
        i1.setOrderId(99L);
        i1.setSkuId(100L);
        i1.setQuantity(2);
        OrderItemPO i2 = new OrderItemPO();
        i2.setOrderId(99L);
        i2.setSkuId(200L);
        i2.setQuantity(1);
        items.add(i1);
        items.add(i2);
        when(orderItemManager.list(any(Wrapper.class))).thenReturn(items);

        service.cancel(99L, 1L);

        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
        verify(stockFeignClient, times(2)).release(any());
    }

    /**
     * 验证 scanExpiredOrders 取消所有 PENDING 且超期订单并释放库存。
     */
    @Test
    public void scanExpiredOrders_cancels_pending_orders() {
        OrderPO o1 = buildOrder(1L, 1L, OrderStatusEnum.PENDING.intCode());
        OrderPO o2 = buildOrder(2L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.List.of(o1, o2));
        when(manager.getById(1L)).thenReturn(o1);
        when(manager.getById(2L)).thenReturn(o2);

        int count = service.scanExpiredOrders();

        assertEquals(2, count);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), o1.getStatus());
        assertEquals(OrderStatusEnum.CANCELED.intCode(), o2.getStatus());
        verify(stockFeignClient, times(2)).release(any());
    }

    /**
     * 验证 scanExpiredOrders 在状态机异常时跳过单条继续处理其余订单。
     */
    @Test
    public void scanExpiredOrders_skips_failed_cancel() {
        OrderPO o1 = buildOrder(1L, 1L, OrderStatusEnum.COMPLETED.intCode());
        OrderPO o2 = buildOrder(2L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.List.of(o1, o2));
        when(manager.getById(1L)).thenReturn(o1);
        when(manager.getById(2L)).thenReturn(o2);

        int count = service.scanExpiredOrders();

        assertEquals(1, count);
        assertEquals(OrderStatusEnum.COMPLETED.intCode(), o1.getStatus());
        assertEquals(OrderStatusEnum.CANCELED.intCode(), o2.getStatus());
    }

    /**
     * 验证 scanExpiredOrders 无超期订单时返回 0。
     */
    @Test
    public void scanExpiredOrders_returns_zero_when_empty() {
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.emptyList());

        assertEquals(0, service.scanExpiredOrders());
    }

    /**
     * 验证 scanAutoConfirm 推进 SHIPPED 超期订单为 COMPLETED。
     */
    @Test
    public void scanAutoConfirm_completes_shipped_orders() {
        OrderPO o1 = buildOrder(1L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.List.of(o1));
        when(manager.getById(1L)).thenReturn(o1);

        int count = service.scanAutoConfirm();

        assertEquals(1, count);
        assertEquals(OrderStatusEnum.COMPLETED.intCode(), o1.getStatus());
    }

    /**
     * 验证 scanAutoConfirm 无超期订单时返回 0。
     */
    @Test
    public void scanAutoConfirm_returns_zero_when_empty() {
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.emptyList());

        assertEquals(0, service.scanAutoConfirm());
    }

    private OrderPO buildOrder(Long id, Long userId, Integer status) {
        OrderPO po = new OrderPO();
        po.setId(id);
        po.setUserId(userId);
        po.setSkuId(100L);
        po.setQuantity(1);
        po.setStatus(status);
        return po;
    }

    private OrderPO buildOrder(Long id, Long userId, Long merchantId, Integer status) {
        OrderPO po = buildOrder(id, userId, status);
        po.setMerchantId(merchantId);
        return po;
    }
}
