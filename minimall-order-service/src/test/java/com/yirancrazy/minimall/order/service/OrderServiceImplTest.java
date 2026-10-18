package com.yirancrazy.minimall.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDTO;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.event.OutboxStore;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusMachine;
import com.yirancrazy.minimall.order.constant.StockReleaseStatusEnum;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDTO;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDTO;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDTO;
import com.yirancrazy.minimall.order.entity.OrderItemPO;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.entity.OrderStatusLogPO;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.manager.OrderItemManager;
import com.yirancrazy.minimall.order.manager.OrderLogisticsManager;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.manager.OrderStatusLogManager;
import com.yirancrazy.minimall.order.service.impl.OrderServiceImpl;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;
import com.yirancrazy.minimall.order.vo.OrderVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class OrderServiceImplTest {

    private OrderManager manager;
    private OrderItemManager orderItemManager;
    private OrderLogisticsManager logisticsManager;
    private OrderStatusLogManager statusLogManager;
    private OrderRemoteGateway orderRemoteGateway;
    private EventBus eventBus;
    private OrderStatusMachine statusMachine;
    private MinioUtil minioUtil;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(OrderManager.class);
        orderItemManager = mock(OrderItemManager.class);
        logisticsManager = mock(OrderLogisticsManager.class);
        statusLogManager = mock(OrderStatusLogManager.class);
        orderRemoteGateway = mock(OrderRemoteGateway.class);
        eventBus = mock(EventBus.class);
        OutboxStore outboxStore = mock(OutboxStore.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        statusMachine = new OrderStatusMachine();
        minioUtil = mock(MinioUtil.class);
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
        lenient().when(statusLogManager.save(any(OrderStatusLogPO.class))).thenReturn(true);
        lenient().when(orderRemoteGateway.skuSnapshot(any())).thenReturn(Result.success(
            new SkuSnapshotDTO(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L)));
        lenient().when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(
            Map.of(100L, new SkuSnapshotDTO(100L, 1L, "sku-100",
                new BigDecimal("9.90"), 100, 10L))));
        lenient().when(orderRemoteGateway.reserve(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(orderRemoteGateway.reserveBatch(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(orderRemoteGateway.release(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(orderRemoteGateway.createPayment(any())).thenReturn(Result.success(2001L));
        lenient().when(orderRemoteGateway.refund(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(orderRemoteGateway.nextId(any())).thenReturn(Result.success(10001L));
        doAnswer(inv -> {
            java.util.function.Consumer<TransactionStatus> action = inv.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        lenient().doAnswer(inv -> {
            java.util.function.Consumer<String> tx = inv.getArgument(1);
            tx.accept("tx-1");
            return null;
        }).when(eventBus).publishInTx(any(), any(java.util.function.Consumer.class), any());
        service = new OrderServiceImpl(manager, orderItemManager, logisticsManager, statusLogManager,
            orderRemoteGateway,
            eventBus, outboxStore, transactionTemplate, statusMachine,
            new com.fasterxml.jackson.databind.ObjectMapper(), minioUtil);
    }


    /**
     * 创建业务数据。
     */
    public void create_sets_pending_status() {
        Long orderId = service.create(1L, 100L, 2);
        assertNotNull(orderId);
        verify(orderRemoteGateway).reserve(any());
        verify(orderRemoteGateway).createPayment(any());
    }


    /**
     * 创建业务数据。
     */
    public void create_saves_order_item_snapshot() {
        service.create(1L, 100L, 2);
        org.mockito.ArgumentCaptor<OrderItemPO> captor =
            org.mockito.ArgumentCaptor.forClass(OrderItemPO.class);
        verify(orderItemManager).save(captor.capture());
        OrderItemPO saved = captor.getValue();
        assertEquals(Long.valueOf(100L), saved.getSkuId());
        assertEquals("sku-100", saved.getSkuName());
        assertEquals(Integer.valueOf(2), saved.getQuantity());
    }


    /**
     * 查询列表数据。
     */
    public void listItemVO_resolves_sku_image_url() {
        OrderItemPO item = new OrderItemPO();
        item.setSpuId(1L);
        item.setSkuId(100L);
        item.setSkuName("耳机");
        item.setSkuImageUrl("img-x.png");
        item.setQuantity(1);
        item.setUnitPrice(new java.math.BigDecimal("10.00"));
        item.setAmount(new java.math.BigDecimal("10.00"));
        when(orderItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
        when(minioUtil.resolvePublicUrl("img-x.png")).thenReturn("http://minio/mall-files/img-x.png?token");
        when(orderRemoteGateway.batchSpuSnapshot(any())).thenReturn(Result.success(
            java.util.Map.of(1L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO(1L, "耳机Pro", null))));

        List<com.yirancrazy.minimall.order.vo.OrderItemVO> result = service.listItemVO(100L);

        assertEquals(1, result.size());
        assertEquals("http://minio/mall-files/img-x.png?token", result.get(0).getSkuImageUrl());
        assertEquals("耳机Pro", result.get(0).getSpuName());
    }


    /**
     * 创建业务数据。
     */
    public void create_snapshot_missing_throws() {
        when(orderRemoteGateway.skuSnapshot(any())).thenReturn(Result.success(null));
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }


    /**
     * 创建业务数据。
     */
    public void create_stock_reserve_fail_throws() {
        when(orderRemoteGateway.reserve(any())).thenReturn(Result.success(Boolean.FALSE));
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }


    /**
     * 创建业务数据。
     */
    public void create_pay_create_fail_throws() {
        when(orderRemoteGateway.createPayment(any())).thenReturn(Result.success(-1L));
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }


    /**
     * 执行 pay_transitions_to_paid 方法。
     */
    public void pay_transitions_to_paid() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.pay(99L);
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }


    /**
     * 执行 pay_missing_order_throws 方法。
     */
    public void pay_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.pay(99L));
    }


    /**
     * 执行 pay_already_paid_is_idempotent 方法。
     */
    public void pay_already_paid_is_idempotent() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.pay(99L);

        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager, never()).updateById(existing);
    }


    /**
     * 执行 pay_refunding_order_throws 方法。
     */
    public void pay_refunding_order_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        BizException exception = assertThrows(BizException.class, () -> service.pay(99L));

        assertEquals(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID.getCode(), exception.getCode());
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        verify(eventBus, never()).publishInTx(any(), any(), any());
    }


    /**
     * 执行 pay_refunded_order_throws 方法。
     */
    public void pay_refunded_order_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        BizException exception = assertThrows(BizException.class, () -> service.pay(99L));

        assertEquals(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID.getCode(), exception.getCode());
        assertEquals(OrderStatusEnum.REFUNDED.intCode(), existing.getStatus());
        verify(eventBus, never()).publishInTx(any(), any(), any());
    }


    /**
     * 执行 payByOrderNo_transitions_to_paid 方法。
     */
    public void payByOrderNo_transitions_to_paid() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(existing);
        when(manager.getById(99L)).thenReturn(existing);

        service.payByOrderNo("OD20260814001");
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }


    /**
     * 执行 payByOrderNo_missing_throws 方法。
     */
    public void payByOrderNo_missing_throws() {
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(null);
        assertThrows(BizException.class, () -> service.payByOrderNo("NOPE"));
    }


    /**
     * 执行 cancel_pending_order_succeeds 方法。
     */
    public void cancel_pending_order_succeeds() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.cancel(99L, 1L);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
    }


    /**
     * 执行 cancel_wrong_user_throws 方法。
     */
    public void cancel_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.cancel(99L, 999L));
    }


    /**
     * 执行 cancel_paid_order_throws 方法。
     */
    public void cancel_paid_order_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.cancel(99L, 1L));
    }


    /**
     * 执行 cancel_refunding_order_throws 方法。
     */
    public void cancel_refunding_order_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.cancel(99L, 1L));
    }


    /**
     * 执行 ship_transitions_to_shipped 方法。
     */
    public void ship_transitions_to_shipped() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.ship(99L, 10L, "顺丰", "SF12345678");
        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
    }


    /**
     * 执行 confirm_transitions_to_received 方法。
     */
    public void confirm_transitions_to_received() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.confirm(99L, 1L);
        assertEquals(OrderStatusEnum.COMPLETED.intCode(), existing.getStatus());
    }


    /**
     * 执行 confirm_wrong_user_throws 方法。
     */
    public void confirm_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.confirm(99L, 999L));
    }


    /**
     * 执行 refund_transitions_to_refunding 方法。
     */
    public void refund_transitions_to_refunding() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.refund(99L, 1L);
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getRefundFromStatus());
    }


    /**
     * 执行 refund_does_not_call_pay_gateway 方法。
     */
    public void refund_does_not_call_pay_gateway() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.refund(99L, 1L);
        verify(orderRemoteGateway, never()).refund(any());
    }


    /**
     * 执行 refund_verifies_order_ownership 方法。
     */
    public void refund_verifies_order_ownership() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.refund(99L, 999L));
    }


    /**
     * 执行 refundCallback_full_refund_transitions_to_refunded 方法。
     */
    public void refundCallback_full_refund_transitions_to_refunded() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("100.00"));
        when(manager.getById(99L)).thenReturn(existing);

        service.handleRefundCallback(refundCallback(99L, "100.00", "100.00", true));

        assertEquals(OrderStatusEnum.REFUNDED.intCode(), existing.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(existing.getTotalRefundedAmount()));
    }


    /**
     * 执行 refundCallback_partial_refund_reverts_and_allows_next_request 方法。
     */
    public void refundCallback_partial_refund_reverts_and_allows_next_request() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.SHIPPED.intCode());
        existing.setPayAmount(new BigDecimal("100.00"));
        existing.setRefundAmount(new BigDecimal("50.00"));
        existing.setRefundRequestNo("REFUND-REQ-1");
        when(manager.getById(99L)).thenReturn(existing);

        service.handleRefundCallback(refundCallback(99L, "50.00", "50.00", false));

        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
        assertEquals(0, new BigDecimal("50.00").compareTo(existing.getTotalRefundedAmount()));
        assertNull(existing.getRefundAmount());
        assertNull(existing.getRefundRequestNo());
    }


    /**
     * 执行 refundCallback_second_partial_refund_finishes_order 方法。
     */
    public void refundCallback_second_partial_refund_finishes_order() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("100.00"));
        existing.setTotalRefundedAmount(new BigDecimal("50.00"));
        when(manager.getById(99L)).thenReturn(existing);

        service.handleRefundCallback(refundCallback(99L, "50.00", "100.00", true));

        assertEquals(OrderStatusEnum.REFUNDED.intCode(), existing.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(existing.getTotalRefundedAmount()));
    }


    /**
     * 处理业务事件。
     */
    public void handleRefundCallback_ignores_duplicate_cumulative_amount() {
        OrderPO existing = buildOrder(100L, 7L, OrderStatusEnum.REFUNDED.intCode());
        existing.setPayAmount(new BigDecimal("100.00"));
        existing.setTotalRefundedAmount(new BigDecimal("100.00"));
        when(manager.getById(100L)).thenReturn(existing);

        service.handleRefundCallback(refundCallback(100L, "50.00", "100.00", true));

        verify(manager, never()).updateById(any(OrderPO.class));
    }


    /**
     * 处理业务事件。
     */
    public void handleRefundCallback_rejects_state_other_than_refunding() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("100.00"));
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class,
            () -> service.handleRefundCallback(refundCallback(99L, "50.00", "50.00", false)));
    }


    /**
     * 获取相关数据。
     */
    public void getStatus_returns_order_status() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        Integer status = service.getStatus(99L, 1L);
        assertEquals(OrderStatusEnum.PAID.intCode(), status);
    }


    /**
     * 获取相关数据。
     */
    public void getStatus_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getStatus(99L, 1L));
    }


    /**
     * 获取相关数据。
     */
    public void getStatus_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.getStatus(99L, 999L));
    }


    /**
     * 分页查询数据。
     */
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
     * 获取相关数据。
     */
    public void getDetail_returns_order_when_exists() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        OrderPO result = service.getDetail(99L, 1L);
        assertEquals(99L, result.getId());
    }


    /**
     * 获取相关数据。
     */
    public void getDetail_missing_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getDetail(99L, 1L));
    }


    /**
     * 获取相关数据。
     */
    public void getDetail_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.getDetail(99L, 999L));
    }


    /**
     * 执行 resolveMerchantId_by_id_returns_merchant 方法。
     */
    public void resolveMerchantId_by_id_returns_merchant() {
        OrderPO existing = buildOrder(99L, 1L, 42L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertEquals(42L, service.resolveMerchantId("99").longValue());
    }


    /**
     * 执行 resolveMerchantId_by_order_no_returns_merchant 方法。
     */
    public void resolveMerchantId_by_order_no_returns_merchant() {
        OrderPO existing = buildOrder(99L, 1L, 42L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(any())).thenReturn(null);
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(existing);

        assertEquals(42L, service.resolveMerchantId("ORD-123").longValue());
    }


    /**
     * 执行 resolveMerchantId_missing_returns_null 方法。
     */
    public void resolveMerchantId_missing_returns_null() {
        when(manager.getById(any())).thenReturn(null);
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(null);

        assertNull(service.resolveMerchantId("NOPE"));
        assertNull(service.resolveMerchantId(null));
    }


    /**
     * 获取相关数据。
     */
    public void getPaySnapshot_returns_order_payment_fields() {
        OrderPO existing = buildOrder(99L, 7L, 42L, OrderStatusEnum.PENDING.intCode());
        existing.setOrderNo("OD99");
        existing.setPayAmount(new BigDecimal("19.80"));
        existing.setPayId(200L);
        when(manager.getOne(any(Wrapper.class))).thenReturn(existing);

        OrderPaySnapshotDTO snapshot = service.getPaySnapshot("OD99");

        assertEquals(99L, snapshot.getOrderId());
        assertEquals("OD99", snapshot.getOrderNo());
        assertEquals(7L, snapshot.getUserId());
        assertEquals(42L, snapshot.getMerchantId());
        assertEquals(0, new BigDecimal("19.80").compareTo(snapshot.getPayAmount()));
        assertEquals(OrderStatusEnum.PENDING.intCode(), snapshot.getStatus());
        assertEquals(200L, snapshot.getPayId());
    }


    /**
     * 获取相关数据。
     */
    public void getPaySnapshot_missing_throws() {
        when(manager.getOne(any(Wrapper.class))).thenReturn(null);
        when(manager.getById(any())).thenReturn(null);

        assertThrows(BizException.class, () -> service.getPaySnapshot("NOPE"));
    }


    /**
     * 执行 merchantClose_pending_succeeds 方法。
     */
    public void merchantClose_pending_succeeds() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.merchantClose(99L, 10L);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
        verify(orderRemoteGateway).release(any());
    }


    /**
     * 执行 merchantClose_wrong_merchant_throws 方法。
     */
    public void merchantClose_wrong_merchant_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.merchantClose(99L, 999L));
    }


    /**
     * 执行 merchantClose_paid_order_throws 方法。
     */
    public void merchantClose_paid_order_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.merchantClose(99L, 10L));
    }


    /**
     * 删除业务数据。
     */
    public void delete_cancelled_succeeds() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.CANCELED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.delete(99L, 1L);
        verify(manager).removeById(99L);
    }


    /**
     * 删除业务数据。
     */
    public void delete_pending_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(99L, 1L));
    }


    /**
     * 删除业务数据。
     */
    public void delete_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.CANCELED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(99L, 999L));
    }


    /**
     * 执行 platformClose_pending_succeeds 方法。
     */
    public void platformClose_pending_succeeds() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.platformClose(99L);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
        verify(orderRemoteGateway).release(any());
    }


    /**
     * 执行 platformClose_paid_throws 方法。
     */
    public void platformClose_paid_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.platformClose(99L));
    }


    /**
     * 执行 platformClose_refunding_throws 方法。
     */
    public void platformClose_refunding_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.platformClose(99L));
    }


    /**
     * 执行 pendingCount_returns_count 方法。
     */
    public void pendingCount_returns_count() {
        when(manager.count(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(5L);
        long count = service.pendingCount(10L);
        assertEquals(5L, count);
    }


    /**
     * 统计业务数据。
     */
    public void countStatusByUser_aggregates_by_status() {
        when(manager.countByStatus(7L)).thenReturn(java.util.List.of(
            statusCount(1, 1L), statusCount(2, 2L), statusCount(3, 1L), statusCount(4, 1L),
            statusCount(5, 3L), statusCount(6, 2L), statusCount(7, 1L)));

        com.yirancrazy.minimall.order.vo.OrderStatusCountsVO vo = service.countStatusByUser(7L);

        assertEquals(1L, vo.getPendingCount());
        assertEquals(2L, vo.getPaidCount());
        assertEquals(1L, vo.getShippedCount());
        assertEquals(1L, vo.getCompletedCount());
        assertEquals(3L, vo.getCanceledCount());
        assertEquals(2L, vo.getRefundingCount());
        assertEquals(1L, vo.getRefundedCount());
    }

    private OrderStatusCountDTO statusCount(int status, long cnt) {
        OrderStatusCountDTO dto = new OrderStatusCountDTO();
        dto.setStatus(status);
        dto.setCnt(cnt);
        return dto;
    }


    /**
     * 统计业务数据。
     */
    public void countStatusByUser_returns_zero_when_empty() {
        when(manager.countByStatus(7L)).thenReturn(java.util.Collections.emptyList());

        com.yirancrazy.minimall.order.vo.OrderStatusCountsVO vo = service.countStatusByUser(7L);

        assertEquals(0L, vo.getPendingCount());
        assertEquals(0L, vo.getPaidCount());
        assertEquals(0L, vo.getShippedCount());
        assertEquals(0L, vo.getCompletedCount());
        assertEquals(0L, vo.getCanceledCount());
        assertEquals(0L, vo.getRefundingCount());
        assertEquals(0L, vo.getRefundedCount());
    }


    /**
     * 执行 orderSummary_delegates_to_mapper_with_parsed_range 方法。
     */
    public void orderSummary_delegates_to_mapper_with_parsed_range() {
        OrderSummaryResultDTO expected = new OrderSummaryResultDTO(
            3L, new BigDecimal("30.00"), new BigDecimal("30.00"), java.math.BigDecimal.ZERO,
            new BigDecimal("5.00"), new BigDecimal("25.00"));
        when(manager.summary(eq(10L), any(), any())).thenReturn(expected);

        com.yirancrazy.minimall.order.vo.OrderSummaryVO vo =
                service.orderSummary("2026-08-01", "2026-08-26", "10");

        assertEquals(3L, vo.getTotalCount());
        assertEquals(0, new BigDecimal("25.00").compareTo(vo.getNetIncome()));
        verify(manager).summary(eq(10L), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class));
    }


    /**
     * 执行 orderSummary_tolerates_invalid_params 方法。
     */
    public void orderSummary_tolerates_invalid_params() {
        OrderSummaryResultDTO expected = new OrderSummaryResultDTO();
        when(manager.summary(isNull(), isNull(), isNull())).thenReturn(expected);

        com.yirancrazy.minimall.order.vo.OrderSummaryVO vo =
                service.orderSummary("bad-date", "2026-13-99", "not-a-number");

        assertNotNull(vo);
        verify(manager).summary(isNull(), isNull(), isNull());
    }


    /**
     * 执行 orderTrend_returns_empty_points_when_no_data 方法。
     */
    public void orderTrend_returns_empty_points_when_no_data() {
        when(manager.trend(isNull(), isNull(), isNull())).thenReturn(null);

        com.yirancrazy.minimall.order.vo.OrderTrendVO vo = service.orderTrend(null, null, null);

        assertNotNull(vo);
        assertTrue(vo.getPoints().isEmpty());
    }


    /**
     * 执行 orderTrend_delegates_to_mapper 方法。
     */
    public void orderTrend_delegates_to_mapper() {
        OrderTrendPointDTO point =
                new OrderTrendPointDTO("2026-08-26", 2L, java.math.BigDecimal.TEN);
        when(manager.trend(eq(1L), any(), any())).thenReturn(java.util.List.of(point));

        com.yirancrazy.minimall.order.vo.OrderTrendVO vo = service.orderTrend("2026-08-01", "2026-08-26", "1");

        assertEquals(1, vo.getPoints().size());
        assertEquals("2026-08-26", vo.getPoints().get(0).getDate());
    }


    /**
     * 执行 exportList_delegates_to_manager 方法。
     */
    public void exportList_delegates_to_manager() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(new java.util.ArrayList<>());
        assertEquals(0, service.exportList(10L, new OrderPageDTO()).size());
    }


    /**
     * 执行 platformExportList_delegates_to_manager 方法。
     */
    public void platformExportList_delegates_to_manager() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(new java.util.ArrayList<>());
        assertEquals(0, service.platformExportList(new OrderPageDTO()).size());
    }


    /**
     * 执行 reviewRefund_approved_triggers_pay 方法。
     */
    public void reviewRefund_approved_triggers_pay() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayId(2001L);
        existing.setAmount(new BigDecimal("19.80"));
        existing.setRefundRequestNo("REFUND-REQ-99");
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, true, 10L, null);
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        org.mockito.ArgumentCaptor<RefundCreateDTO> captor =
            org.mockito.ArgumentCaptor.forClass(RefundCreateDTO.class);
        verify(orderRemoteGateway).refund(captor.capture());
        assertEquals("REFUND-REQ-99", captor.getValue().getRefundRequestNo());
    }


    /**
     * 执行 reviewRefund_pay_returns_false_throws_and_keeps_refunding 方法。
     */
    public void reviewRefund_pay_returns_false_throws_and_keeps_refunding() {
        OrderPO existing = buildOrder(100L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setPayId(200L);
        existing.setAmount(new BigDecimal("50.00"));
        existing.setRefundRequestNo("REFUND-REQ-100");
        when(manager.getById(100L)).thenReturn(existing);
        when(orderRemoteGateway.refund(any())).thenReturn(Result.success(Boolean.FALSE));

        BizException ex = assertThrows(BizException.class,
            () -> service.reviewRefund(100L, true, 10L, "ok"));

        assertEquals("50007", ex.getCode());
        verify(manager, never()).updateById(any(OrderPO.class));
    }


    /**
     * 执行 reviewRefund_rejected_reverts_status 方法。
     */
    public void reviewRefund_rejected_reverts_status() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, false, 10L, "证据不足");
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }


    /**
     * 执行 reviewRefund_wrong_merchant_throws 方法。
     */
    public void reviewRefund_wrong_merchant_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reviewRefund(99L, true, 999L, null));
    }


    /**
     * 执行 reviewRefund_not_refunding_throws 方法。
     */
    public void reviewRefund_not_refunding_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reviewRefund(99L, true, 10L, null));
    }


    /**
     * 执行 merchantInitiateRefund_partial_amount_persisted 方法。
     */
    public void merchantInitiateRefund_partial_amount_persisted() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("19.80"));
        existing.setAmount(new BigDecimal("19.80"));
        when(manager.getById(99L)).thenReturn(existing);

        service.merchantInitiateRefund(99L, 10L, "5.00", "部分退货");

        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        assertEquals(0, new BigDecimal("5.00").compareTo(existing.getRefundAmount()));
    }


    /**
     * 执行 merchantInitiateRefund_completed_throws 方法。
     */
    public void merchantInitiateRefund_completed_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.COMPLETED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class,
            () -> service.merchantInitiateRefund(99L, 10L, "19.80", "售后退款"));
    }


    /**
     * 执行 merchantInitiateRefund_amount_exceeds_pay_throws 方法。
     */
    public void merchantInitiateRefund_amount_exceeds_pay_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("19.80"));
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class,
            () -> service.merchantInitiateRefund(99L, 10L, "20.00", "超额退款"));
    }


    /**
     * 执行 merchantInitiateRefund_rejects_more_than_two_decimals 方法。
     */
    public void merchantInitiateRefund_rejects_more_than_two_decimals() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("19.80"));
        when(manager.getById(99L)).thenReturn(existing);

        BizException ex = assertThrows(BizException.class,
            () -> service.merchantInitiateRefund(99L, 10L, "5.005", "精度非法"));

        assertEquals(OrderCodeEnum.REFUND_AMOUNT_INVALID.getCode(), ex.getCode());
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
    }


    /**
     * 执行 reviewRefund_approved_uses_partial_refund_amount 方法。
     */
    public void reviewRefund_approved_uses_partial_refund_amount() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setPayId(2001L);
        existing.setAmount(new BigDecimal("19.80"));
        existing.setRefundAmount(new BigDecimal("5.00"));
        existing.setRefundRequestNo("REFUND-REQ-99");
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, true, 10L, null);

        org.mockito.ArgumentCaptor<RefundCreateDTO> captor =
            org.mockito.ArgumentCaptor.forClass(RefundCreateDTO.class);
        verify(orderRemoteGateway).refund(captor.capture());
        assertEquals(2001L, captor.getValue().getPayId().longValue());
        assertEquals(0, new BigDecimal("5.00").compareTo(captor.getValue().getAmount()));
        assertEquals("REFUND-REQ-99", captor.getValue().getRefundRequestNo());
    }


    /**
     * 执行 arbitrateRefund_approved_submits_pay_refund 方法。
     */
    public void arbitrateRefund_approved_submits_pay_refund() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayId(2001L);
        existing.setAmount(new BigDecimal("19.80"));
        existing.setRefundAmount(new BigDecimal("5.00"));
        when(manager.getById(99L)).thenReturn(existing);

        service.arbitrateRefund(99L, true);

        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        org.mockito.ArgumentCaptor<RefundCreateDTO> captor =
            org.mockito.ArgumentCaptor.forClass(RefundCreateDTO.class);
        verify(orderRemoteGateway).refund(captor.capture());
        assertEquals(2001L, captor.getValue().getPayId().longValue());
        assertEquals(0, new BigDecimal("5.00").compareTo(captor.getValue().getAmount()));
    }


    /**
     * 执行 arbitrateRefund_approved_pay_failure_keeps_refunding 方法。
     */
    public void arbitrateRefund_approved_pay_failure_keeps_refunding() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayId(2001L);
        existing.setAmount(new BigDecimal("19.80"));
        when(manager.getById(99L)).thenReturn(existing);
        when(orderRemoteGateway.refund(any())).thenReturn(Result.success(Boolean.FALSE));

        assertThrows(BizException.class, () -> service.arbitrateRefund(99L, true));
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
    }


    /**
     * 执行 arbitrateRefund_rejected_reverts_status 方法。
     */
    public void arbitrateRefund_rejected_reverts_status() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.arbitrateRefund(99L, false);
        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }


    /**
     * 执行 arbitrateRefund_not_refunding_throws 方法。
     */
    public void arbitrateRefund_not_refunding_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.arbitrateRefund(99L, true));
    }


    /**
     * 执行 queryLogistics_returns_nodes_when_order_exists 方法。
     */
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

        List<OrderLogisticsVO> result = service.queryLogistics(99L, 1L);
        assertEquals(2, result.size());
        assertEquals("已发货", result.get(0).getNode());
        assertEquals("已签收", result.get(1).getNode());
    }


    /**
     * 执行 queryLogistics_missing_order_throws 方法。
     */
    public void queryLogistics_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.queryLogistics(99L, 1L));
    }


    /**
     * 执行 queryLogistics_wrong_user_throws 方法。
     */
    public void queryLogistics_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.queryLogistics(99L, 999L));
    }


    /**
     * 执行 ship_inserts_logistics_node 方法。
     */
    public void ship_inserts_logistics_node() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.ship(99L, 10L, "顺丰", "SF12345678");
        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
        verify(logisticsManager).save(any(OrderLogisticsPO.class));
    }


    /**
     * 校验业务条件。
     */
    public void checkout_multi_sku_succeeds() {
        List<OrderCheckoutItemDTO> items = List.of(
            new OrderCheckoutItemDTO(100L, 2),
            new OrderCheckoutItemDTO(200L, 1));
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(Map.of(
            100L, new SkuSnapshotDTO(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L),
            200L, new SkuSnapshotDTO(200L, 1L, "sku-200", new BigDecimal("5.00"), 100, 10L))));

        Long orderId = service.checkout(1L, items);

        assertNotNull(orderId);
        verify(orderRemoteGateway).batchSkuSnapshot(any());
        verify(orderRemoteGateway).reserveBatch(any());
        verify(orderItemManager, times(2)).save(any(OrderItemPO.class));
        verify(orderRemoteGateway).createPayment(any());
    }


    /**
     * 校验业务条件。
     */
    public void checkout_cross_merchant_splits_orders() {
        OrderCheckoutItemDTO itemA = new OrderCheckoutItemDTO(100L, 1);
        OrderCheckoutItemDTO itemB = new OrderCheckoutItemDTO(200L, 1);
        SkuSnapshotDTO snapA = new SkuSnapshotDTO(100L, 1L, "sku-A", new BigDecimal("10.00"), 100, 10L);
        SkuSnapshotDTO snapB = new SkuSnapshotDTO(200L, 2L, "sku-B", new BigDecimal("20.00"), 100, 20L);
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(Map.of(
            100L, snapA, 200L, snapB)));

        Long mainOrderId = service.checkout(1L, List.of(itemA, itemB));

        assertNotNull(mainOrderId);
        verify(orderRemoteGateway).reserveBatch(any());
        verify(orderItemManager, times(2)).save(any(OrderItemPO.class));
        verify(orderRemoteGateway, times(2)).createPayment(any());
        verify(manager, times(2)).save(any(OrderPO.class));
    }


    /**
     * 校验业务条件。
     */
    public void checkout_snapshot_without_merchant_throws() {
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(Map.of(
            100L, new SkuSnapshotDTO(100L, 1L, "sku-100",
                new BigDecimal("9.90"), 100, null))));
        List<OrderCheckoutItemDTO> items = List.of(new OrderCheckoutItemDTO(100L, 1));

        assertThrows(BizException.class, () -> service.checkout(1L, items));
    }


    /**
     * 校验业务条件。
     */
    public void checkout_empty_items_throws() {
        assertThrows(BizException.class, () -> service.checkout(1L, java.util.Collections.emptyList()));
    }


    /**
     * 校验业务条件。
     */
    public void checkout_fails_when_id_service_unavailable() {
        when(orderRemoteGateway.nextId(anyString())).thenReturn(Result.fail("20000", "ID服务不可用"));
        List<OrderCheckoutItemDTO> items = List.of(new OrderCheckoutItemDTO(100L, 1));

        BizException ex = assertThrows(BizException.class, () -> service.checkout(1L, items));

        assertEquals(OrderCodeEnum.ORDER_ID_GENERATE_FAIL.getCode(), ex.getCode());
        verify(manager, never()).save(any(OrderPO.class));
    }


    /**
     * 执行 commentEligibility_eligible_uses_item_merchant_snapshot 方法。
     */
    public void commentEligibility_eligible_uses_item_merchant_snapshot() {
        OrderPO po = buildOrder(99L, 7L, OrderStatusEnum.COMPLETED.intCode());
        po.setOrderNo("OD99");
        po.setMerchantId(10L);
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);
        OrderItemPO item = new OrderItemPO();
        item.setOrderId(99L);
        item.setSpuId(500L);
        item.setMerchantId(77L);
        when(orderItemManager.getOne(any(Wrapper.class))).thenReturn(item);

        OrderCommentEligibilityDTO dto = service.commentEligibility("OD99", 7L, 500L);

        assertTrue(dto.isEligible());
        assertEquals(77L, dto.getMerchantId());
        assertEquals(99L, dto.getOrderId());
    }


    /**
     * 执行 commentEligibility_rejects_other_user_order 方法。
     */
    public void commentEligibility_rejects_other_user_order() {
        OrderPO po = buildOrder(99L, 7L, OrderStatusEnum.PAID.intCode());
        po.setOrderNo("OD99");
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);

        OrderCommentEligibilityDTO dto = service.commentEligibility("OD99", 999L, 500L);

        assertFalse(dto.isEligible());
        verify(orderItemManager, never()).getOne(any(Wrapper.class));
    }


    /**
     * 执行 commentEligibility_rejects_unpaid_order 方法。
     */
    public void commentEligibility_rejects_unpaid_order() {
        OrderPO po = buildOrder(99L, 7L, OrderStatusEnum.PENDING.intCode());
        po.setOrderNo("OD99");
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);

        OrderCommentEligibilityDTO dto = service.commentEligibility("OD99", 7L, 500L);

        assertFalse(dto.isEligible());
        verify(orderItemManager, never()).getOne(any(Wrapper.class));
    }


    /**
     * 执行 commentEligibility_rejects_spu_not_in_order 方法。
     */
    public void commentEligibility_rejects_spu_not_in_order() {
        OrderPO po = buildOrder(99L, 7L, OrderStatusEnum.SHIPPED.intCode());
        po.setOrderNo("OD99");
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);
        when(orderItemManager.getOne(any(Wrapper.class))).thenReturn(null);

        OrderCommentEligibilityDTO dto = service.commentEligibility("OD99", 7L, 500L);

        assertFalse(dto.isEligible());
    }


    /**
     * 校验业务条件。
     */
    public void checkout_snapshot_missing_throws() {
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(Map.of()));
        List<OrderCheckoutItemDTO> items = List.of(new OrderCheckoutItemDTO(100L, 1));

        assertThrows(BizException.class, () -> service.checkout(1L, items));
    }


    /**
     * 校验业务条件。
     */
    public void checkout_stock_reserve_fail_throws() {
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(Map.of(
            100L, new SkuSnapshotDTO(100L, 1L, "sku-100",
                new BigDecimal("9.90"), 100, 10L))));
        when(orderRemoteGateway.reserveBatch(any())).thenReturn(Result.success(Boolean.FALSE));
        List<OrderCheckoutItemDTO> items = List.of(new OrderCheckoutItemDTO(100L, 1));

        assertThrows(BizException.class, () -> service.checkout(1L, items));
    }


    /**
     * 校验业务条件。
     */
    public void checkout_100_skus_uses_batch_calls_once() {
        List<OrderCheckoutItemDTO> items = new ArrayList<>(100);
        java.util.Map<Long, SkuSnapshotDTO> snapshots = new java.util.HashMap<>(200);
        for (long skuId = 1L; skuId <= 100L; skuId++) {
            items.add(new OrderCheckoutItemDTO(skuId, 1));
            snapshots.put(skuId, new SkuSnapshotDTO(skuId, skuId, "sku-" + skuId,
                new BigDecimal("1.00"), 100, 10L));
        }
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(snapshots));

        service.checkout(1L, items);

        verify(orderRemoteGateway).batchSkuSnapshot(any());
        verify(orderRemoteGateway).reserveBatch(any());
    }


    /**
     * 校验业务条件。
     */
    public void checkout_merges_duplicate_skus() {
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(Map.of(
            100L, new SkuSnapshotDTO(100L, 1L, "sku-100",
                new BigDecimal("9.90"), 100, 10L))));
        List<OrderCheckoutItemDTO> items = List.of(
            new OrderCheckoutItemDTO(100L, 2),
            new OrderCheckoutItemDTO(100L, 3));

        service.checkout(1L, items);

        ArgumentCaptor<StockBatchReserveDTO> captor =
            ArgumentCaptor.forClass(StockBatchReserveDTO.class);
        verify(orderRemoteGateway).reserveBatch(captor.capture());
        assertEquals(1, captor.getValue().getItems().size());
        assertEquals(100L, captor.getValue().getItems().get(0).getSkuId());
        assertEquals(5, captor.getValue().getItems().get(0).getQuantity());
    }


    /**
     * 校验业务条件。
     */
    public void checkout_rejects_more_than_100_unique_skus() {
        List<OrderCheckoutItemDTO> items = new ArrayList<>(101);
        for (long skuId = 1L; skuId <= 101L; skuId++) {
            items.add(new OrderCheckoutItemDTO(skuId, 1));
        }

        BizException ex = assertThrows(BizException.class, () -> service.checkout(1L, items));

        assertEquals(OrderCodeEnum.ORDER_ITEMS_LIMIT_EXCEEDED.getCode(), ex.getCode());
        verifyNoInteractions(orderRemoteGateway);
    }


    /**
     * 执行 cancel_multi_sku_releases_all_items 方法。
     */
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
        verify(orderRemoteGateway, times(2)).release(any());
    }


    /**
     * 执行 transitStatus_throws_when_update_returns_zero_rows 方法。
     */
    public void transitStatus_throws_when_update_returns_zero_rows() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);
        when(manager.updateById(any(OrderPO.class))).thenReturn(false);

        assertThrows(BizException.class, () -> service.cancel(99L, 1L));
        verify(orderRemoteGateway, never()).release(any());
        verify(statusLogManager, never()).save(any(OrderStatusLogPO.class));
    }


    /**
     * 扫描并处理待办数据。
     */
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
        verify(orderRemoteGateway, times(2)).release(any());
    }


    /**
     * 扫描并处理待办数据。
     */
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
     * 扫描并处理待办数据。
     */
    public void scanExpiredOrders_returns_zero_when_empty() {
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.emptyList());

        assertEquals(0, service.scanExpiredOrders());
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanExpiredOrders_scans_in_cursor_batches() {
        java.util.List<OrderPO> fullBatch = new java.util.ArrayList<>(200);
        for (long id = 1L; id <= 200L; id++) {
            OrderPO po = new OrderPO();
            po.setId(id);
            po.setStatus(OrderStatusEnum.PENDING.intCode());
            fullBatch.add(po);
        }
        when(manager.list(any(Wrapper.class))).thenReturn(fullBatch, java.util.Collections.emptyList());

        service.scanExpiredOrders();

        verify(manager, times(2)).list(any(Wrapper.class));
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanStockReleasePending_marks_released_after_success() {
        OrderPO po = buildOrder(100L, 1L, OrderStatusEnum.CANCELED.intCode());
        po.setOrderNo("O100");
        po.setStockReleaseStatus(StockReleaseStatusEnum.PENDING.intCode());
        po.setSkuId(100L);
        po.setQuantity(2);
        when(manager.list(any(Wrapper.class))).thenReturn(List.of(po));
        when(orderRemoteGateway.release(any())).thenReturn(Result.success(Boolean.TRUE));

        int count = service.scanStockReleasePending();

        assertEquals(1, count);
        assertEquals(StockReleaseStatusEnum.RELEASED.intCode(), po.getStockReleaseStatus());
        verify(manager).updateById(po);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanStockReleasePending_refreshes_failed_order() {
        OrderPO po = buildOrder(100L, 1L, OrderStatusEnum.CANCELED.intCode());
        po.setOrderNo("O100");
        po.setStockReleaseStatus(StockReleaseStatusEnum.PENDING.intCode());
        po.setSkuId(100L);
        po.setQuantity(2);
        when(manager.list(any(Wrapper.class))).thenReturn(List.of(po));
        when(orderRemoteGateway.release(any())).thenReturn(Result.success(Boolean.FALSE));

        int count = service.scanStockReleasePending();

        assertEquals(0, count);
        assertEquals(StockReleaseStatusEnum.PENDING.intCode(), po.getStockReleaseStatus());
        verify(manager).updateById(po);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanAutoConfirm_completes_shipped_orders() {
        OrderPO o1 = buildOrder(1L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.List.of(o1));
        when(manager.getById(1L)).thenReturn(o1);

        int count = service.scanAutoConfirm();

        assertEquals(1, count);
        assertEquals(OrderStatusEnum.COMPLETED.intCode(), o1.getStatus());
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanAutoConfirm_returns_zero_when_empty() {
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.emptyList());

        assertEquals(0, service.scanAutoConfirm());
    }


    /**
     * 创建业务数据。
     */
    public void create_sets_order_metadata() {
        service.create(1L, 100L, 2);

        org.mockito.ArgumentCaptor<OrderPO> captor = org.mockito.ArgumentCaptor.forClass(OrderPO.class);
        verify(manager).save(captor.capture());
        OrderPO po = captor.getValue();
        assertNotNull(po.getOrderNo());
        assertEquals(0, new BigDecimal("19.80").compareTo(po.getTotalAmount()));
        assertEquals(0, new BigDecimal("19.80").compareTo(po.getPayAmount()));
        assertNotNull(po.getPayExpireAt());
    }


    /**
     * 创建业务数据。
     */
    public void create_fails_when_id_service_unavailable() {
        when(orderRemoteGateway.nextId(anyString())).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> service.create(1L, 100L, 2));

        assertEquals(OrderCodeEnum.ORDER_ID_GENERATE_FAIL.getCode(), ex.getCode());
        verify(manager, never()).save(any(OrderPO.class));
    }


    /**
     * 执行 pay_sets_paid_at_and_writes_status_log 方法。
     */
    public void pay_sets_paid_at_and_writes_status_log() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.pay(99L);

        assertNotNull(existing.getPaidAt());
        verify(statusLogManager).save(any(OrderStatusLogPO.class));
    }


    /**
     * 执行 cancel_sets_closed_at_and_reason 方法。
     */
    public void cancel_sets_closed_at_and_reason() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.cancel(99L, 1L);

        assertNotNull(existing.getClosedAt());
        assertEquals("USER_CANCEL", existing.getCloseReason());
        verify(statusLogManager).save(any(OrderStatusLogPO.class));
    }


    /**
     * 校验业务条件。
     */
    public void checkout_writes_item_snapshot() {
        List<OrderCheckoutItemDTO> items = List.of(new OrderCheckoutItemDTO(100L, 2));
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(Map.of(
            100L, new SkuSnapshotDTO(100L, 1L, "sku-100",
                new BigDecimal("9.90"), 100, 10L))));

        service.checkout(1L, items);

        org.mockito.ArgumentCaptor<OrderItemPO> captor = org.mockito.ArgumentCaptor.forClass(OrderItemPO.class);
        verify(orderItemManager).save(captor.capture());
        OrderItemPO item = captor.getValue();
        assertNotNull(item.getSkuSnapshotJson());
        assertNotNull(item.getSpuSnapshotJson());
        assertEquals(10L, item.getMerchantId().longValue());
        assertEquals(0, new BigDecimal("19.80").compareTo(item.getSubtotalAmount()));
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

    private RefundCallbackDTO refundCallback(Long orderId, String refundedAmount,
                                             String totalRefundedAmount, boolean fullyRefunded) {
        return new RefundCallbackDTO(orderId, 1L, new BigDecimal(refundedAmount),
            new BigDecimal(totalRefundedAmount), fullyRefunded, "REFUND-REQ-1");
    }


    /**
     * 执行 merchantPageVO_enriches_skuName_and_rejectReason 方法。
     */
    public void merchantPageVO_enriches_skuName_and_rejectReason() {
        OrderPO po = buildOrder(88L, 1L, 10L, OrderStatusEnum.REFUNDED.intCode());
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.singletonList(po));
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(
            java.util.Map.of(100L, new SkuSnapshotDTO(100L, 1L, "iPhone 15", new BigDecimal("5999.00"), 10, 10L))));
        OrderStatusLogPO log = new OrderStatusLogPO();
        log.setOrderId(88L);
        log.setNote("证据不足");
        when(statusLogManager.list(any(Wrapper.class))).thenReturn(java.util.Collections.singletonList(log));

        CursorPageVO<OrderVO> result = service.merchantPageVO(new OrderPageDTO());

        OrderVO vo = result.getRecords().get(0);
        assertEquals("iPhone 15", vo.getSkuName());
        assertEquals("证据不足", vo.getRejectReason());
        assertEquals("REFUNDED", vo.getStatus());
    }


    /**
     * 执行 merchantPageVO_fills_checkout_quantity_and_skuName_from_items 方法。
     */
    public void merchantPageVO_fills_checkout_quantity_and_skuName_from_items() {
        OrderPO po = buildOrder(90L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        po.setSkuId(null);
        po.setQuantity(null);
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.singletonList(po));

        OrderItemPO item1 = new OrderItemPO();
        item1.setOrderId(90L);
        item1.setSpuId(1L);
        item1.setSkuName("Redmi Note");
        item1.setQuantity(2);
        OrderItemPO item2 = new OrderItemPO();
        item2.setOrderId(90L);
        item2.setSpuId(2L);
        item2.setSkuName("iPhone 15");
        item2.setQuantity(3);
        OrderItemPO item3 = new OrderItemPO();
        item3.setOrderId(90L);
        item3.setSpuId(3L);
        item3.setSkuName("Pixel 9");
        item3.setQuantity(1);
        when(orderItemManager.list(any(Wrapper.class))).thenReturn(java.util.List.of(item1, item2, item3));
        when(orderRemoteGateway.batchSpuSnapshot(any())).thenReturn(Result.success(
            java.util.Map.of(1L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO(1L, "小米", null),
                             2L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO(2L, "苹果", null))));

        CursorPageVO<OrderVO> result = service.merchantPageVO(new OrderPageDTO());

        OrderVO vo = result.getRecords().get(0);
        assertEquals(Integer.valueOf(6), vo.getQuantity());
        assertEquals("小米 | Redmi Note * 2、苹果 | iPhone 15 * 3…", vo.getSkuName());
    }


    /**
     * 执行 merchantDetailVO_returns_enriched_order 方法。
     */
    public void merchantDetailVO_returns_enriched_order() {
        OrderPO po = buildOrder(91L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        po.setSkuId(null);
        po.setQuantity(null);
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);

        OrderItemPO item = new OrderItemPO();
        item.setOrderId(91L);
        item.setSpuId(1L);
        item.setSkuName("Redmi Note");
        item.setQuantity(2);
        when(orderItemManager.list(any(Wrapper.class))).thenReturn(java.util.Collections.singletonList(item));
        when(orderRemoteGateway.batchSpuSnapshot(any())).thenReturn(Result.success(
            java.util.Map.of(1L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO(1L, "小米", null))));

        OrderVO vo = service.merchantDetailVO(91L, 10L);

        assertEquals(Integer.valueOf(2), vo.getQuantity());
        assertEquals("小米 | Redmi Note * 2", vo.getSkuName());
    }


    /**
     * 执行 merchantDetailVO_throws_when_order_not_found 方法。
     */
    public void merchantDetailVO_throws_when_order_not_found() {
        when(manager.getOne(any(Wrapper.class))).thenReturn(null);

        assertThrows(BizException.class, () -> service.merchantDetailVO(99L, 10L));
    }
}
