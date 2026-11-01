package com.yirancrazy.minimall.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDto;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.order.constant.CartCleanupStatusEnum;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusMachine;
import com.yirancrazy.minimall.order.constant.StockReleaseStatusEnum;
import com.yirancrazy.minimall.order.dto.CheckoutResultDto;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDto;
import com.yirancrazy.minimall.order.dto.OrderPageDto;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDto;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDto;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDto;
import com.yirancrazy.minimall.order.entity.CartCleanupTaskPo;
import com.yirancrazy.minimall.order.entity.OrderItemPo;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPo;
import com.yirancrazy.minimall.order.entity.OrderPo;
import com.yirancrazy.minimall.order.entity.OrderStatusLogPo;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.manager.CartCleanupTaskManager;
import com.yirancrazy.minimall.order.manager.OrderItemManager;
import com.yirancrazy.minimall.order.manager.OrderLogisticsManager;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.manager.OrderStatusLogManager;
import com.yirancrazy.minimall.order.service.impl.OrderServiceImpl;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVo;
import com.yirancrazy.minimall.order.vo.OrderVo;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 覆盖 OrderServiceImpl 的创建、状态、退款、查询、统计和扫描核心业务。
 *   业务边界：使用 Manager、远程网关、发件箱服务和事务模板的 Mockito 桩，状态机使用真实实现。
 *   使用场景：通过捕获持久化参数、异常码和 Mockito 交互验证正常、失败及边界路径。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class OrderServiceImplTest {

    private OrderManager manager;
    private OrderItemManager orderItemManager;
    private OrderLogisticsManager logisticsManager;
    private OrderStatusLogManager statusLogManager;
    private OrderRemoteGateway orderRemoteGateway;
    private CartCleanupTaskManager cartCleanupTaskManager;
    private OrderOutboxService orderOutboxService;
    private OrderStatusMachine statusMachine;
    private MinioUtil minioUtil;
    private OrderServiceImpl service;

    /**
     * 初始化当前测试类的共享夹具。
     *
     * <p>按测试类型创建 Mockito 桩、事务模板替身或 MockMvc，并为成功路径设置宽松默认值。
     */
    @BeforeEach
    void setUp() {
        manager = mock(OrderManager.class);
        orderItemManager = mock(OrderItemManager.class);
        logisticsManager = mock(OrderLogisticsManager.class);
        statusLogManager = mock(OrderStatusLogManager.class);
        orderRemoteGateway = mock(OrderRemoteGateway.class);
        cartCleanupTaskManager = mock(CartCleanupTaskManager.class);
        orderOutboxService = mock(OrderOutboxService.class);
        final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        statusMachine = new OrderStatusMachine();
        minioUtil = mock(MinioUtil.class);
        lenient().when(manager.updateById(any(OrderPo.class))).thenReturn(true);
        lenient().when(manager.removeById(any(Long.class))).thenReturn(true);
        doAnswer(
                        inv -> {
                            OrderPo p = inv.getArgument(0);
                            if (p.getId() == null) {
                                p.setId(System.nanoTime());
                            }
                            return true;
                        })
                .when(manager)
                .save(any(OrderPo.class));
        lenient().when(orderItemManager.save(any(OrderItemPo.class))).thenReturn(true);
        lenient()
                .when(orderItemManager.list(any(Wrapper.class)))
                .thenReturn(java.util.Collections.emptyList());
        lenient().when(logisticsManager.save(any(OrderLogisticsPo.class))).thenReturn(true);
        lenient().when(statusLogManager.save(any(OrderStatusLogPo.class))).thenReturn(true);
        lenient()
                .doAnswer(
                        inv -> {
                            CartCleanupTaskPo task = inv.getArgument(0);
                            task.setId(3001L);
                            return true;
                        })
                .when(cartCleanupTaskManager)
                .save(any(CartCleanupTaskPo.class));
        lenient()
                .when(orderRemoteGateway.skuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                new SkuSnapshotDto(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L)));
        lenient()
                .when(orderRemoteGateway.batchSkuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                Map.of(
                                        100L,
                                        new SkuSnapshotDto(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L))));
        lenient().when(orderRemoteGateway.reserve(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(orderRemoteGateway.reserveBatch(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(orderRemoteGateway.release(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(orderRemoteGateway.createPayment(any())).thenReturn(Result.success(2001L));
        lenient().when(orderRemoteGateway.refund(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(orderRemoteGateway.nextId(any())).thenReturn(Result.success(10001L));
        doAnswer(
                        inv -> {
                            java.util.function.Consumer<TransactionStatus> action = inv.getArgument(0);
                            action.accept(mock(TransactionStatus.class));
                            return null;
                        })
                .when(transactionTemplate)
                .executeWithoutResult(any());
        service =
                new OrderServiceImpl(
                        manager,
                        orderItemManager,
                        logisticsManager,
                        statusLogManager,
                        orderRemoteGateway,
                        orderOutboxService,
                        transactionTemplate,
                        statusMachine,
                        new com.fasterxml.jackson.databind.ObjectMapper(),
                        minioUtil,
                        cartCleanupTaskManager);
    }

    /** 验证创建订单时初始化待支付状态. */
    @Test
    public void testCreateSetsPendingStatus() {
        Long orderId = service.create(1L, 100L, 2);
        assertNotNull(orderId);
        verify(orderRemoteGateway).reserve(any());
        verify(orderRemoteGateway).createPayment(any());
    }

    /** 验证创建订单时使用最终订单号预占库存. */
    @Test
    void testCreateReservesStockWithFinalOrderNo() {
        service.create(1L, 100L, 2);

        ArgumentCaptor<StockReserveDto> reserveCaptor = ArgumentCaptor.forClass(StockReserveDto.class);
        verify(orderRemoteGateway).reserve(reserveCaptor.capture());
        ArgumentCaptor<OrderPo> orderCaptor = ArgumentCaptor.forClass(OrderPo.class);
        verify(manager).save(orderCaptor.capture());

        assertNotNull(reserveCaptor.getValue().getOrderNo());
        assertEquals(orderCaptor.getValue().getOrderNo(), reserveCaptor.getValue().getOrderNo());
    }

    /** 验证创建保存订单条目快照. */
    @Test
    public void testCreateSavesOrderItemSnapshot() {
        service.create(1L, 100L, 2);
        org.mockito.ArgumentCaptor<OrderItemPo> captor =
                org.mockito.ArgumentCaptor.forClass(OrderItemPo.class);
        verify(orderItemManager).save(captor.capture());
        OrderItemPo saved = captor.getValue();
        assertEquals(Long.valueOf(100L), saved.getSkuId());
        assertEquals("sku-100", saved.getSkuName());
        assertEquals(Integer.valueOf(2), saved.getQuantity());
    }

    /** 验证订单明细视图解析 SKU 图片公开地址. */
    @Test
    public void testListItemVoResolvesSkuImageUrl() {
        OrderItemPo item = new OrderItemPo();
        item.setSpuId(1L);
        item.setSkuId(100L);
        item.setSkuName("耳机");
        item.setSkuImageUrl("img-x.png");
        item.setQuantity(1);
        item.setUnitPrice(new java.math.BigDecimal("10.00"));
        item.setAmount(new java.math.BigDecimal("10.00"));
        when(orderItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));
        when(minioUtil.resolvePublicUrl("img-x.png"))
                .thenReturn("http://minio/mall-files/img-x.png?token");
        when(orderRemoteGateway.batchSpuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                java.util.Map.of(
                                        1L,
                                        new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto(1L, "耳机Pro", null))));

        List<com.yirancrazy.minimall.order.vo.OrderItemVo> result = service.listItemVo(100L);

        assertEquals(1, result.size());
        assertEquals("http://minio/mall-files/img-x.png?token", result.get(0).getSkuImageUrl());
        assertEquals("耳机Pro", result.get(0).getSpuName());
    }

    /** 验证SKU 快照缺失时创建订单抛出业务异常. */
    @Test
    public void testCreateSnapshotMissingThrows() {
        when(orderRemoteGateway.skuSnapshot(any())).thenReturn(Result.success(null));
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }

    /** 验证库存预占失败时创建订单抛出业务异常. */
    @Test
    public void testCreateStockReserveFailThrows() {
        when(orderRemoteGateway.reserve(any())).thenReturn(Result.success(Boolean.FALSE));
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }

    /** 验证支付流水创建失败时创建订单抛出业务异常. */
    @Test
    public void testCreatePayCreateFailThrows() {
        when(orderRemoteGateway.createPayment(any())).thenReturn(Result.success(-1L));
        assertThrows(BizException.class, () -> service.create(1L, 100L, 2));
    }

    /** 验证支付后订单状态流转为已支付. */
    @Test
    public void testPayTransitionsToPaid() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.pay(99L);
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
        verify(orderOutboxService).recordPaidEvent(any(OrderPaidDto.class));
    }

    /** 验证发件箱写入失败向上传播. */
    @Test
    public void testPayOutboxFailurePropagates() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);
        doThrow(new IllegalStateException("outbox failed"))
                .when(orderOutboxService)
                .recordPaidEvent(any(OrderPaidDto.class));

        assertThrows(IllegalStateException.class, () -> service.pay(99L));

        verify(orderOutboxService).recordPaidEvent(any(OrderPaidDto.class));
    }

    /** 验证订单不存在时支付抛出业务异常. */
    @Test
    public void testPayMissingOrderThrows() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.pay(99L));
    }

    /** 验证已支付订单重复支付保持幂等. */
    @Test
    public void testPayAlreadyPaidIsIdempotent() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.pay(99L);

        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager, never()).updateById(existing);
        verify(orderOutboxService, never()).recordPaidEvent(any());
    }

    /** 验证退款中订单支付时抛出业务异常. */
    @Test
    public void testPayRefundingOrderThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        BizException exception = assertThrows(BizException.class, () -> service.pay(99L));

        assertEquals(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID.getCode(), exception.getCode());
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        verify(orderOutboxService, never()).recordPaidEvent(any());
    }

    /** 验证已退款订单支付时抛出业务异常. */
    @Test
    public void testPayRefundedOrderThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        BizException exception = assertThrows(BizException.class, () -> service.pay(99L));

        assertEquals(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID.getCode(), exception.getCode());
        assertEquals(OrderStatusEnum.REFUNDED.intCode(), existing.getStatus());
        verify(orderOutboxService, never()).recordPaidEvent(any());
    }

    /** 验证按订单号支付时状态流转到已支付. */
    @Test
    public void testPayByOrderNoTransitionsToPaid() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(existing);
        when(manager.getById(99L)).thenReturn(existing);

        service.payByOrderNo("OD20260814001");
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }

    /** 验证订单号不存在时支付抛出业务异常. */
    @Test
    public void testPayByOrderNoMissingThrows() {
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);
        assertThrows(BizException.class, () -> service.payByOrderNo("NOPE"));
    }

    /** 验证取消待处理订单成功. */
    @Test
    public void testCancelPendingOrderSucceeds() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.cancel(99L, 1L);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
    }

    /** 验证取消错误用户抛出. */
    @Test
    public void testCancelWrongUserThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.cancel(99L, 999L));
    }

    /** 验证已支付订单取消时抛出业务异常. */
    @Test
    public void testCancelPaidOrderThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.cancel(99L, 1L));
    }

    /** 验证退款中订单取消时抛出业务异常. */
    @Test
    public void testCancelRefundingOrderThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.cancel(99L, 1L));
    }

    /** 验证发货流转到已发货. */
    @Test
    public void testShipTransitionsToShipped() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.ship(99L, 10L, "顺丰", "SF12345678");
        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
    }

    /** 验证确认收货后订单状态流转为已完成. */
    @Test
    public void testConfirmTransitionsToReceived() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.confirm(99L, 1L);
        assertEquals(OrderStatusEnum.COMPLETED.intCode(), existing.getStatus());
    }

    /** 验证非订单所属用户确认收货时抛出业务异常. */
    @Test
    public void testConfirmWrongUserThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.confirm(99L, 999L));
    }

    /** 验证退款流转到退款中. */
    @Test
    public void testRefundTransitionsToRefunding() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.refund(99L, 1L);
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getRefundFromStatus());
    }

    /** 验证退款不调用支付网关. */
    @Test
    public void testRefundDoesNotCallPayGateway() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.refund(99L, 1L);
        verify(orderRemoteGateway, never()).refund(any());
    }

    /** 验证退款校验订单归属关系. */
    @Test
    public void testRefundVerifiesOrderOwnership() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.refund(99L, 999L));
    }

    /** 验证退款回调完整退款流转到已退款. */
    @Test
    public void testRefundCallbackFullRefundTransitionsToRefunded() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("100.00"));
        when(manager.getById(99L)).thenReturn(existing);

        service.handleRefundCallback(refundCallback(99L, "100.00", "100.00", true));

        assertEquals(OrderStatusEnum.REFUNDED.intCode(), existing.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(existing.getTotalRefundedAmount()));
    }

    /** 验证退款回调部分退款回滚并允许下一个请求. */
    @Test
    public void testRefundCallbackPartialRefundRevertsAndAllowsNextRequest() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
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

    /** 验证退款回调第二次部分退款完成订单. */
    @Test
    public void testRefundCallbackSecondPartialRefundFinishesOrder() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("100.00"));
        existing.setTotalRefundedAmount(new BigDecimal("50.00"));
        when(manager.getById(99L)).thenReturn(existing);

        service.handleRefundCallback(refundCallback(99L, "50.00", "100.00", true));

        assertEquals(OrderStatusEnum.REFUNDED.intCode(), existing.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(existing.getTotalRefundedAmount()));
    }

    /** 验证退款回调忽略重复累计金额. */
    @Test
    public void testHandleRefundCallbackIgnoresDuplicateCumulativeAmount() {
        OrderPo existing = buildOrder(100L, 7L, OrderStatusEnum.REFUNDED.intCode());
        existing.setPayAmount(new BigDecimal("100.00"));
        existing.setTotalRefundedAmount(new BigDecimal("100.00"));
        when(manager.getById(100L)).thenReturn(existing);

        service.handleRefundCallback(refundCallback(100L, "50.00", "100.00", true));

        verify(manager, never()).updateById(any(OrderPo.class));
    }

    /** 验证非退款中状态拒绝退款回调. */
    @Test
    public void testHandleRefundCallbackRejectsStateOtherThanRefunding() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("100.00"));
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(
                BizException.class,
                () -> service.handleRefundCallback(refundCallback(99L, "50.00", "50.00", false)));
    }

    /** 验证获取状态返回订单状态. */
    @Test
    public void testGetStatusReturnsOrderStatus() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        Integer status = service.getStatus(99L, 1L);
        assertEquals(OrderStatusEnum.PAID.intCode(), status);
    }

    /** 验证获取状态缺失订单抛出. */
    @Test
    public void testGetStatusMissingOrderThrows() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getStatus(99L, 1L));
    }

    /** 验证获取状态错误用户抛出. */
    @Test
    public void testGetStatusWrongUserThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.getStatus(99L, 999L));
    }

    /** 验证分页委派到管理器. */
    @Test
    public void testPageDelegatesToManager() {
        OrderPageDto dto = new OrderPageDto();
        dto.setUserId(1L);
        List<OrderPo> mockRecords = new ArrayList<>();
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(mockRecords);

        CursorPageVo<OrderPo> result = service.page(dto);
        assertNotNull(result);
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /** 验证订单存在时返回详情. */
    @Test
    public void testGetDetailReturnsOrderWhenExists() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        OrderPo result = service.getDetail(99L, 1L);
        assertEquals(99L, result.getId());
    }

    /** 验证获取详情缺失抛出. */
    @Test
    public void testGetDetailMissingThrows() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getDetail(99L, 1L));
    }

    /** 验证获取详情错误用户抛出. */
    @Test
    public void testGetDetailWrongUserThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.getDetail(99L, 999L));
    }

    /** 验证按订单ID解析归属商家. */
    @Test
    public void testResolveMerchantIdByIdReturnsMerchant() {
        OrderPo existing = buildOrder(99L, 1L, 42L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertEquals(42L, service.resolveMerchantId("99").longValue());
    }

    /** 验证按订单号解析归属商家. */
    @Test
    public void testResolveMerchantIdByOrderNoReturnsMerchant() {
        OrderPo existing = buildOrder(99L, 1L, 42L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(any())).thenReturn(null);
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(existing);

        assertEquals(42L, service.resolveMerchantId("ORD-123").longValue());
    }

    /** 验证解析商家ID缺失返回空. */
    @Test
    public void testResolveMerchantIdMissingReturnsNull() {
        when(manager.getById(any())).thenReturn(null);
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(null);

        assertNull(service.resolveMerchantId("NOPE"));
        assertNull(service.resolveMerchantId(null));
    }

    /** 验证获取支付快照返回订单支付字段. */
    @Test
    public void testGetPaySnapshotReturnsOrderPaymentFields() {
        OrderPo existing = buildOrder(99L, 7L, 42L, OrderStatusEnum.PENDING.intCode());
        existing.setOrderNo("OD99");
        existing.setPayAmount(new BigDecimal("19.80"));
        existing.setPayId(200L);
        when(manager.getOne(any(Wrapper.class))).thenReturn(existing);

        OrderPaySnapshotDto snapshot = service.getPaySnapshot("OD99");

        assertEquals(99L, snapshot.getOrderId());
        assertEquals("OD99", snapshot.getOrderNo());
        assertEquals(7L, snapshot.getUserId());
        assertEquals(42L, snapshot.getMerchantId());
        assertEquals(0, new BigDecimal("19.80").compareTo(snapshot.getPayAmount()));
        assertEquals(OrderStatusEnum.PENDING.intCode(), snapshot.getStatus());
        assertEquals(200L, snapshot.getPayId());
    }

    /** 验证获取支付快照缺失抛出. */
    @Test
    public void testGetPaySnapshotMissingThrows() {
        when(manager.getOne(any(Wrapper.class))).thenReturn(null);
        when(manager.getById(any())).thenReturn(null);

        assertThrows(BizException.class, () -> service.getPaySnapshot("NOPE"));
    }

    /** 验证商家关闭待处理成功. */
    @Test
    public void testMerchantClosePendingSucceeds() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.merchantClose(99L, 10L);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
        verify(orderRemoteGateway).release(any());
    }

    /** 验证非归属商家关闭订单时抛出业务异常. */
    @Test
    public void testMerchantCloseWrongMerchantThrows() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.merchantClose(99L, 999L));
    }

    /** 验证商家关闭已支付订单时抛出业务异常. */
    @Test
    public void testMerchantClosePaidOrderThrows() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.merchantClose(99L, 10L));
    }

    /** 验证已取消订单可成功删除. */
    @Test
    public void testDeleteCancelledSucceeds() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.CANCELED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.delete(99L, 1L);
        verify(manager).removeById(99L);
    }

    /** 验证待支付订单删除时抛出业务异常. */
    @Test
    public void testDeletePendingThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(99L, 1L));
    }

    /** 验证非订单所属用户删除时抛出业务异常. */
    @Test
    public void testDeleteWrongUserThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.CANCELED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(99L, 999L));
    }

    /** 验证平台关闭待处理成功. */
    @Test
    public void testPlatformClosePendingSucceeds() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.platformClose(99L);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), existing.getStatus());
        verify(orderRemoteGateway).release(any());
    }

    /** 验证平台关闭已支付抛出. */
    @Test
    public void testPlatformClosePaidThrows() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.platformClose(99L));
    }

    /** 验证平台关闭退款中抛出. */
    @Test
    public void testPlatformCloseRefundingThrows() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.platformClose(99L));
    }

    /** 验证待办订单计数返回数据库统计值. */
    @Test
    public void testPendingCountReturnsCount() {
        when(manager.count(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(5L);
        long count = service.pendingCount(10L);
        assertEquals(5L, count);
    }

    /** 验证用户订单状态计数按状态聚合. */
    @Test
    public void testCountStatusByUserAggregatesByStatus() {
        when(manager.countByStatus(7L))
                .thenReturn(
                        java.util.List.of(
                                statusCount(1, 1L),
                                statusCount(2, 2L),
                                statusCount(3, 1L),
                                statusCount(4, 1L),
                                statusCount(5, 3L),
                                statusCount(6, 2L),
                                statusCount(7, 1L)));

        com.yirancrazy.minimall.order.vo.OrderStatusCountsVo vo = service.countStatusByUser(7L);

        assertEquals(1L, vo.getPendingCount());
        assertEquals(2L, vo.getPaidCount());
        assertEquals(1L, vo.getShippedCount());
        assertEquals(1L, vo.getCompletedCount());
        assertEquals(3L, vo.getCanceledCount());
        assertEquals(2L, vo.getRefundingCount());
        assertEquals(1L, vo.getRefundedCount());
    }

    /**
     * 构造状态计数数据库投影。
     *
     * <p>设置状态编码和数量，用于验证用户状态汇总映射。
     *
     * @param status 订单状态筛选值或状态编码
     * @param cnt 指定订单状态的记录数量
     * @return 状态计数 DTO。
     */
    private OrderStatusCountDto statusCount(int status, long cnt) {
        OrderStatusCountDto dto = new OrderStatusCountDto();
        dto.setStatus(status);
        dto.setCnt(cnt);
        return dto;
    }

    /** 验证用户状态计数为空时各项返回零. */
    @Test
    public void testCountStatusByUserReturnsZeroWhenEmpty() {
        when(manager.countByStatus(7L)).thenReturn(java.util.Collections.emptyList());

        com.yirancrazy.minimall.order.vo.OrderStatusCountsVo vo = service.countStatusByUser(7L);

        assertEquals(0L, vo.getPendingCount());
        assertEquals(0L, vo.getPaidCount());
        assertEquals(0L, vo.getShippedCount());
        assertEquals(0L, vo.getCompletedCount());
        assertEquals(0L, vo.getCanceledCount());
        assertEquals(0L, vo.getRefundingCount());
        assertEquals(0L, vo.getRefundedCount());
    }

    /** 验证订单汇总委派到映射器使用已解析范围. */
    @Test
    public void testOrderSummaryDelegatesToMapperWithParsedRange() {
        OrderSummaryResultDto expected =
                new OrderSummaryResultDto(
                        3L,
                        new BigDecimal("30.00"),
                        new BigDecimal("30.00"),
                        java.math.BigDecimal.ZERO,
                        new BigDecimal("5.00"),
                        new BigDecimal("25.00"));
        when(manager.summary(eq(10L), any(), any())).thenReturn(expected);

        com.yirancrazy.minimall.order.vo.OrderSummaryVo vo =
                service.orderSummary("2026-08-01", "2026-08-26", "10");

        assertEquals(3L, vo.getTotalCount());
        assertEquals(0, new BigDecimal("25.00").compareTo(vo.getNetIncome()));
        verify(manager)
                .summary(eq(10L), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class));
    }

    /** 验证订单汇总容忍无效参数. */
    @Test
    public void testOrderSummaryToleratesInvalidParams() {
        OrderSummaryResultDto expected = new OrderSummaryResultDto();
        when(manager.summary(isNull(), isNull(), isNull())).thenReturn(expected);

        com.yirancrazy.minimall.order.vo.OrderSummaryVo vo =
                service.orderSummary("bad-date", "2026-13-99", "not-a-number");

        assertNotNull(vo);
        verify(manager).summary(isNull(), isNull(), isNull());
    }

    /** 验证订单趋势无数据时返回空点位. */
    @Test
    public void testOrderTrendReturnsEmptyPointsWhenNoData() {
        when(manager.trend(isNull(), isNull(), isNull())).thenReturn(null);

        com.yirancrazy.minimall.order.vo.OrderTrendVo vo = service.orderTrend(null, null, null);

        assertNotNull(vo);
        assertTrue(vo.getPoints().isEmpty());
    }

    /** 验证订单趋势委派到映射器. */
    @Test
    public void testOrderTrendDelegatesToMapper() {
        OrderTrendPointDto point = new OrderTrendPointDto("2026-08-26", 2L, java.math.BigDecimal.TEN);
        when(manager.trend(eq(1L), any(), any())).thenReturn(java.util.List.of(point));

        com.yirancrazy.minimall.order.vo.OrderTrendVo vo =
                service.orderTrend("2026-08-01", "2026-08-26", "1");

        assertEquals(1, vo.getPoints().size());
        assertEquals("2026-08-26", vo.getPoints().get(0).getDate());
    }

    /** 验证导出列表委派到管理器. */
    @Test
    public void testExportListDelegatesToManager() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(new java.util.ArrayList<>());
        assertEquals(0, service.exportList(10L, new OrderPageDto()).size());
    }

    /** 验证平台导出列表委派到管理器. */
    @Test
    public void testPlatformExportListDelegatesToManager() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(new java.util.ArrayList<>());
        assertEquals(0, service.platformExportList(new OrderPageDto()).size());
    }

    /** 验证审核退款已通过触发支付. */
    @Test
    public void testReviewRefundApprovedTriggersPay() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayId(2001L);
        existing.setAmount(new BigDecimal("19.80"));
        existing.setRefundRequestNo("REFUND-REQ-99");
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, true, 10L, null);
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        org.mockito.ArgumentCaptor<RefundCreateDto> captor =
                org.mockito.ArgumentCaptor.forClass(RefundCreateDto.class);
        verify(orderRemoteGateway).refund(captor.capture());
        assertEquals("REFUND-REQ-99", captor.getValue().getRefundRequestNo());
    }

    /** 验证支付退款返回失败时抛异常并保持退款中. */
    @Test
    public void testReviewRefundPayReturnsFalseThrowsAndKeepsRefunding() {
        OrderPo existing = buildOrder(100L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setPayId(200L);
        existing.setAmount(new BigDecimal("50.00"));
        existing.setRefundRequestNo("REFUND-REQ-100");
        when(manager.getById(100L)).thenReturn(existing);
        when(orderRemoteGateway.refund(any())).thenReturn(Result.success(Boolean.FALSE));

        BizException ex =
                assertThrows(BizException.class, () -> service.reviewRefund(100L, true, 10L, "ok"));

        assertEquals("50007", ex.getCode());
        verify(manager, never()).updateById(any(OrderPo.class));
    }

    /** 验证审核退款被拒绝回滚状态. */
    @Test
    public void testReviewRefundRejectedRevertsStatus() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, false, 10L, "证据不足");
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }

    /** 验证非归属商家审核退款时抛出业务异常. */
    @Test
    public void testReviewRefundWrongMerchantThrows() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reviewRefund(99L, true, 999L, null));
    }

    /** 验证非退款中订单审核时抛出业务异常. */
    @Test
    public void testReviewRefundNotRefundingThrows() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reviewRefund(99L, true, 10L, null));
    }

    /** 验证商家部分退款金额正确持久化. */
    @Test
    public void testMerchantInitiateRefundPartialAmountPersisted() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("19.80"));
        existing.setAmount(new BigDecimal("19.80"));
        when(manager.getById(99L)).thenReturn(existing);

        service.merchantInitiateRefund(99L, 10L, "5.00", "部分退货");

        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        assertEquals(0, new BigDecimal("5.00").compareTo(existing.getRefundAmount()));
    }

    /** 验证已完成订单发起退款时抛出业务异常. */
    @Test
    public void testMerchantInitiateRefundCompletedThrows() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.COMPLETED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(
                BizException.class, () -> service.merchantInitiateRefund(99L, 10L, "19.80", "售后退款"));
    }

    /** 验证商家退款金额超过可退金额时抛出业务异常. */
    @Test
    public void testMerchantInitiateRefundAmountExceedsPayThrows() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("19.80"));
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(
                BizException.class, () -> service.merchantInitiateRefund(99L, 10L, "20.00", "超额退款"));
    }

    /** 验证商家退款拒绝超过两位小数. */
    @Test
    public void testMerchantInitiateRefundRejectsMoreThanTwoDecimals() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("19.80"));
        when(manager.getById(99L)).thenReturn(existing);

        BizException ex =
                assertThrows(
                        BizException.class, () -> service.merchantInitiateRefund(99L, 10L, "5.005", "精度非法"));

        assertEquals(OrderCodeEnum.REFUND_AMOUNT_INVALID.getCode(), ex.getCode());
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
    }

    /** 验证审核退款已通过使用部分退款金额. */
    @Test
    public void testReviewRefundApprovedUsesPartialRefundAmount() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setPayId(2001L);
        existing.setAmount(new BigDecimal("19.80"));
        existing.setRefundAmount(new BigDecimal("5.00"));
        existing.setRefundRequestNo("REFUND-REQ-99");
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, true, 10L, null);

        org.mockito.ArgumentCaptor<RefundCreateDto> captor =
                org.mockito.ArgumentCaptor.forClass(RefundCreateDto.class);
        verify(orderRemoteGateway).refund(captor.capture());
        assertEquals(2001L, captor.getValue().getPayId().longValue());
        assertEquals(0, new BigDecimal("5.00").compareTo(captor.getValue().getAmount()));
        assertEquals("REFUND-REQ-99", captor.getValue().getRefundRequestNo());
    }

    /** 验证仲裁通过后提交支付退款. */
    @Test
    public void testArbitrateRefundApprovedSubmitsPayRefund() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayId(2001L);
        existing.setAmount(new BigDecimal("19.80"));
        existing.setRefundAmount(new BigDecimal("5.00"));
        when(manager.getById(99L)).thenReturn(existing);

        service.arbitrateRefund(99L, true);

        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        org.mockito.ArgumentCaptor<RefundCreateDto> captor =
                org.mockito.ArgumentCaptor.forClass(RefundCreateDto.class);
        verify(orderRemoteGateway).refund(captor.capture());
        assertEquals(2001L, captor.getValue().getPayId().longValue());
        assertEquals(0, new BigDecimal("5.00").compareTo(captor.getValue().getAmount()));
    }

    /** 验证仲裁通过但支付失败时保持退款中. */
    @Test
    public void testArbitrateRefundApprovedPayFailureKeepsRefunding() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayId(2001L);
        existing.setAmount(new BigDecimal("19.80"));
        when(manager.getById(99L)).thenReturn(existing);
        when(orderRemoteGateway.refund(any())).thenReturn(Result.success(Boolean.FALSE));

        assertThrows(BizException.class, () -> service.arbitrateRefund(99L, true));
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
    }

    /** 验证仲裁驳回后回滚到退款前状态. */
    @Test
    public void testArbitrateRefundRejectedRevertsStatus() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.arbitrateRefund(99L, false);
        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }

    /** 验证订单非退款中时仲裁抛出业务异常. */
    @Test
    public void testArbitrateRefundNotRefundingThrows() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.arbitrateRefund(99L, true));
    }

    /** 验证订单存在时返回物流节点. */
    @Test
    public void testQueryLogisticsReturnsNodesWhenOrderExists() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);
        final List<OrderLogisticsPo> nodes = new ArrayList<>(2);
        OrderLogisticsPo first = new OrderLogisticsPo();
        first.setOrderId(99L);
        first.setNode("已发货");
        first.setDescription("商家已发货");
        first.setCreateTime(LocalDateTime.of(2026, 8, 3, 10, 0));
        OrderLogisticsPo second = new OrderLogisticsPo();
        second.setOrderId(99L);
        second.setNode("已签收");
        second.setDescription("用户已签收");
        second.setCreateTime(LocalDateTime.of(2026, 8, 4, 12, 0));
        nodes.add(first);
        nodes.add(second);
        when(logisticsManager.list(any(Wrapper.class))).thenReturn(nodes);

        List<OrderLogisticsVo> result = service.queryLogistics(99L, 1L);
        assertEquals(2, result.size());
        assertEquals("已发货", result.get(0).getNode());
        assertEquals("已签收", result.get(1).getNode());
    }

    /** 验证查询物流缺失订单抛出. */
    @Test
    public void testQueryLogisticsMissingOrderThrows() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.queryLogistics(99L, 1L));
    }

    /** 验证查询物流错误用户抛出. */
    @Test
    public void testQueryLogisticsWrongUserThrows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.queryLogistics(99L, 999L));
    }

    /** 验证发货成功后插入物流节点. */
    @Test
    public void testShipInsertsLogisticsNode() {
        OrderPo existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.ship(99L, 10L, "顺丰", "SF12345678");
        assertEquals(OrderStatusEnum.SHIPPED.intCode(), existing.getStatus());
        verify(logisticsManager).save(any(OrderLogisticsPo.class));
    }

    /** 验证结算多SKU成功. */
    @Test
    public void testCheckoutMultiSkuSucceeds() {
        List<OrderCheckoutItemDto> items =
                List.of(new OrderCheckoutItemDto(100L, 2), new OrderCheckoutItemDto(200L, 1));
        when(orderRemoteGateway.batchSkuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                Map.of(
                                        100L, new SkuSnapshotDto(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L),
                                        200L,
                                                new SkuSnapshotDto(
                                                        200L, 1L, "sku-200", new BigDecimal("5.00"), 100, 10L))));

        Long orderId = service.checkout(1L, items);

        assertNotNull(orderId);
        verify(orderRemoteGateway).batchSkuSnapshot(any());
        verify(orderRemoteGateway).reserveBatch(any());
        verify(manager).save(any(OrderPo.class));
        verify(orderItemManager, times(2)).save(any(OrderItemPo.class));
        verify(orderRemoteGateway).createPayment(any());
    }

    /** 验证结算预占库存使用最终订单号. */
    @Test
    void testCheckoutReservesStockWithFinalOrderNo() {
        service.checkout(1L, List.of(new OrderCheckoutItemDto(100L, 2)));

        ArgumentCaptor<StockBatchReserveDto> reserveCaptor =
                ArgumentCaptor.forClass(StockBatchReserveDto.class);
        verify(orderRemoteGateway).reserveBatch(reserveCaptor.capture());
        ArgumentCaptor<OrderPo> orderCaptor = ArgumentCaptor.forClass(OrderPo.class);
        verify(manager).save(orderCaptor.capture());

        StockReserveDto reserveItem = reserveCaptor.getValue().getItems().get(0);
        assertNotNull(reserveItem.getOrderNo());
        assertEquals(orderCaptor.getValue().getOrderNo(), reserveItem.getOrderNo());
    }

    /** 验证结算使用清理条目创建清理任务. */
    @Test
    void testCheckoutWithCleanupItemsCreatesCleanupTask() throws Exception {
        List<OrderCheckoutItemDto> orderItems = List.of(new OrderCheckoutItemDto(100L, 2));
        List<CartCheckoutItemDto> cleanupItems = List.of(new CartCheckoutItemDto(9001L, 100L, 2));

        CheckoutResultDto result = service.checkout(7L, orderItems, cleanupItems, null);

        assertEquals(3001L, result.getCartCleanupTaskId());
        ArgumentCaptor<CartCleanupTaskPo> captor = ArgumentCaptor.forClass(CartCleanupTaskPo.class);
        verify(cartCleanupTaskManager).save(captor.capture());
        CartCleanupTaskPo task = captor.getValue();
        assertEquals(7L, task.getUserId());
        assertEquals(CartCleanupStatusEnum.PENDING.intCode(), task.getStatus());
        com.fasterxml.jackson.databind.JsonNode items =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(task.getItemsJson());
        assertEquals(9001L, items.get(0).get("cartItemId").asLong());
        assertEquals(100L, items.get(0).get("skuId").asLong());
        assertEquals(2, items.get(0).get("quantity").asInt());
    }

    /** 验证无清理条目时结算不创建清理任务. */
    @Test
    void testCheckoutWithoutCleanupItemsDoesNotCreateTask() {
        CheckoutResultDto result =
                service.checkout(7L, List.of(new OrderCheckoutItemDto(100L, 1)), List.of(), null);

        assertNull(result.getCartCleanupTaskId());
        verify(cartCleanupTaskManager, never()).save(any());
    }

    /** 验证跨商家结算在产生副作用前终止. */
    @Test
    public void testCheckoutCrossMerchantThrowsBeforeSideEffects() {
        OrderCheckoutItemDto itemA = new OrderCheckoutItemDto(100L, 1);
        OrderCheckoutItemDto itemB = new OrderCheckoutItemDto(200L, 1);
        SkuSnapshotDto snapA = new SkuSnapshotDto(100L, 1L, "sku-A", new BigDecimal("10.00"), 100, 10L);
        SkuSnapshotDto snapB = new SkuSnapshotDto(200L, 2L, "sku-B", new BigDecimal("20.00"), 100, 20L);
        when(orderRemoteGateway.batchSkuSnapshot(any()))
                .thenReturn(Result.success(Map.of(100L, snapA, 200L, snapB)));
        List<CartCheckoutItemDto> cleanupItems = List.of(new CartCheckoutItemDto(9001L, 100L, 1));

        BizException exception =
                assertThrows(
                        BizException.class,
                        () -> service.checkout(1L, List.of(itemA, itemB), cleanupItems, null));

        assertEquals(OrderCodeEnum.ORDER_MULTI_MERCHANT_NOT_ALLOWED.getCode(), exception.getCode());
        verify(orderRemoteGateway, never()).nextId(anyString());
        verify(orderRemoteGateway, never()).reserveBatch(any());
        verify(manager, never()).save(any(OrderPo.class));
        verify(orderItemManager, never()).save(any(OrderItemPo.class));
        verify(orderRemoteGateway, never()).createPayment(any());
        verify(cartCleanupTaskManager, never()).save(any(CartCleanupTaskPo.class));
    }

    /** 验证SKU 快照缺少商家时结算抛出业务异常. */
    @Test
    public void testCheckoutSnapshotWithoutMerchantThrows() {
        when(orderRemoteGateway.batchSkuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                Map.of(
                                        100L,
                                        new SkuSnapshotDto(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, null))));
        List<OrderCheckoutItemDto> items = List.of(new OrderCheckoutItemDto(100L, 1));

        assertThrows(BizException.class, () -> service.checkout(1L, items));
        verify(orderRemoteGateway, never()).reserveBatch(any());
        verify(manager, never()).save(any(OrderPo.class));
    }

    /** 验证结算空条目抛出. */
    @Test
    public void testCheckoutEmptyItemsThrows() {
        assertThrows(BizException.class, () -> service.checkout(1L, java.util.Collections.emptyList()));
    }

    /** 验证ID 服务不可用时结算失败. */
    @Test
    public void testCheckoutFailsWhenIdServiceUnavailable() {
        when(orderRemoteGateway.nextId(anyString())).thenReturn(Result.fail("20000", "ID服务不可用"));
        List<OrderCheckoutItemDto> items = List.of(new OrderCheckoutItemDto(100L, 1));

        BizException ex = assertThrows(BizException.class, () -> service.checkout(1L, items));

        assertEquals(OrderCodeEnum.ORDER_ID_GENERATE_FAIL.getCode(), ex.getCode());
        verify(manager, never()).save(any(OrderPo.class));
    }

    /** 验证评价资格优先使用订单明细商家快照. */
    @Test
    public void testCommentEligibilityEligibleUsesItemMerchantSnapshot() {
        OrderPo po = buildOrder(99L, 7L, OrderStatusEnum.COMPLETED.intCode());
        po.setOrderNo("OD99");
        po.setMerchantId(10L);
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);
        OrderItemPo item = new OrderItemPo();
        item.setOrderId(99L);
        item.setSpuId(500L);
        item.setMerchantId(77L);
        when(orderItemManager.getOne(any(Wrapper.class))).thenReturn(item);

        OrderCommentEligibilityDto dto = service.commentEligibility("OD99", 7L, 500L);

        assertTrue(dto.isEligible());
        assertEquals(77L, dto.getMerchantId());
        assertEquals(99L, dto.getOrderId());
    }

    /** 验证评论资格拒绝其他用户订单. */
    @Test
    public void testCommentEligibilityRejectsOtherUserOrder() {
        OrderPo po = buildOrder(99L, 7L, OrderStatusEnum.PAID.intCode());
        po.setOrderNo("OD99");
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);

        OrderCommentEligibilityDto dto = service.commentEligibility("OD99", 999L, 500L);

        assertFalse(dto.isEligible());
        verify(orderItemManager, never()).getOne(any(Wrapper.class));
    }

    /** 验证评论资格拒绝未支付订单. */
    @Test
    public void testCommentEligibilityRejectsUnpaidOrder() {
        OrderPo po = buildOrder(99L, 7L, OrderStatusEnum.PENDING.intCode());
        po.setOrderNo("OD99");
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);

        OrderCommentEligibilityDto dto = service.commentEligibility("OD99", 7L, 500L);

        assertFalse(dto.isEligible());
        verify(orderItemManager, never()).getOne(any(Wrapper.class));
    }

    /** 验证订单不包含目标 SPU 时拒绝评价资格. */
    @Test
    public void testCommentEligibilityRejectsSpuNotInOrder() {
        OrderPo po = buildOrder(99L, 7L, OrderStatusEnum.SHIPPED.intCode());
        po.setOrderNo("OD99");
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);
        when(orderItemManager.getOne(any(Wrapper.class))).thenReturn(null);

        OrderCommentEligibilityDto dto = service.commentEligibility("OD99", 7L, 500L);

        assertFalse(dto.isEligible());
    }

    /** 验证SKU 快照缺失时结算抛出业务异常. */
    @Test
    public void testCheckoutSnapshotMissingThrows() {
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(Map.of()));
        List<OrderCheckoutItemDto> items = List.of(new OrderCheckoutItemDto(100L, 1));

        assertThrows(BizException.class, () -> service.checkout(1L, items));
    }

    /** 验证批量库存预占失败时结算抛出业务异常. */
    @Test
    public void testCheckoutStockReserveFailThrows() {
        when(orderRemoteGateway.batchSkuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                Map.of(
                                        100L,
                                        new SkuSnapshotDto(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L))));
        when(orderRemoteGateway.reserveBatch(any())).thenReturn(Result.success(Boolean.FALSE));
        List<OrderCheckoutItemDto> items = List.of(new OrderCheckoutItemDto(100L, 1));

        assertThrows(BizException.class, () -> service.checkout(1L, items));
    }

    /** 验证结算 100 个 SKU 时各批量接口只调用一次. */
    @Test
    public void testCheckout100SkusUsesBatchCallsOnce() {
        List<OrderCheckoutItemDto> items = new ArrayList<>(100);
        java.util.Map<Long, SkuSnapshotDto> snapshots = new java.util.HashMap<>(200);
        for (long skuId = 1L; skuId <= 100L; skuId++) {
            items.add(new OrderCheckoutItemDto(skuId, 1));
            snapshots.put(
                    skuId,
                    new SkuSnapshotDto(skuId, skuId, "sku-" + skuId, new BigDecimal("1.00"), 100, 10L));
        }
        when(orderRemoteGateway.batchSkuSnapshot(any())).thenReturn(Result.success(snapshots));

        service.checkout(1L, items);

        verify(orderRemoteGateway).batchSkuSnapshot(any());
        verify(orderRemoteGateway).reserveBatch(any());
    }

    /** 验证结算时合并重复 SKU 数量. */
    @Test
    public void testCheckoutMergesDuplicateSkus() {
        when(orderRemoteGateway.batchSkuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                Map.of(
                                        100L,
                                        new SkuSnapshotDto(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L))));
        List<OrderCheckoutItemDto> items =
                List.of(new OrderCheckoutItemDto(100L, 2), new OrderCheckoutItemDto(100L, 3));

        service.checkout(1L, items);

        ArgumentCaptor<StockBatchReserveDto> captor =
                ArgumentCaptor.forClass(StockBatchReserveDto.class);
        verify(orderRemoteGateway).reserveBatch(captor.capture());
        assertEquals(1, captor.getValue().getItems().size());
        assertEquals(100L, captor.getValue().getItems().get(0).getSkuId());
        assertEquals(5, captor.getValue().getItems().get(0).getQuantity());
    }

    /** 验证结算拒绝超过 100 个唯一 SKU. */
    @Test
    public void testCheckoutRejectsMoreThan100UniqueSkus() {
        List<OrderCheckoutItemDto> items = new ArrayList<>(101);
        for (long skuId = 1L; skuId <= 101L; skuId++) {
            items.add(new OrderCheckoutItemDto(skuId, 1));
        }

        BizException ex = assertThrows(BizException.class, () -> service.checkout(1L, items));

        assertEquals(OrderCodeEnum.ORDER_ITEMS_LIMIT_EXCEEDED.getCode(), ex.getCode());
        verifyNoInteractions(orderRemoteGateway);
    }

    /** 验证取消多SKU释放全部条目. */
    @Test
    public void testCancelMultiSkuReleasesAllItems() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);
        final List<OrderItemPo> items = new ArrayList<>(2);
        OrderItemPo i1 = new OrderItemPo();
        i1.setOrderId(99L);
        i1.setSkuId(100L);
        i1.setQuantity(2);
        OrderItemPo i2 = new OrderItemPo();
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

    /** 验证取消订单时使用订单号释放库存. */
    @Test
    void testCancelReleasesStockWithOrderNo() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        existing.setOrderNo("OD99");
        when(manager.getById(99L)).thenReturn(existing);

        service.cancel(99L, 1L);

        ArgumentCaptor<StockReserveDto> releaseCaptor = ArgumentCaptor.forClass(StockReserveDto.class);
        verify(orderRemoteGateway).release(releaseCaptor.capture());
        assertEquals("OD99", releaseCaptor.getValue().getOrderNo());
    }

    /** 验证状态更新影响零行时抛出业务异常. */
    @Test
    public void testTransitStatusThrowsWhenUpdateReturnsZeroRows() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);
        when(manager.updateById(any(OrderPo.class))).thenReturn(false);

        assertThrows(BizException.class, () -> service.cancel(99L, 1L));
        verify(orderRemoteGateway, never()).release(any());
        verify(statusLogManager, never()).save(any(OrderStatusLogPo.class));
    }

    /** 验证扫描过期订单取消待处理订单. */
    @Test
    public void testScanExpiredOrdersCancelsPendingOrders() {
        OrderPo o1 = buildOrder(1L, 1L, OrderStatusEnum.PENDING.intCode());
        OrderPo o2 = buildOrder(2L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.listExpiredPendingOrders(eq(OrderStatusEnum.PENDING.intCode()), eq(0L), eq(200)))
                .thenReturn(java.util.List.of(o1, o2));
        when(manager.getById(1L)).thenReturn(o1);
        when(manager.getById(2L)).thenReturn(o2);

        int count = service.scanExpiredOrders();

        assertEquals(2, count);
        assertEquals(OrderStatusEnum.CANCELED.intCode(), o1.getStatus());
        assertEquals(OrderStatusEnum.CANCELED.intCode(), o2.getStatus());
        verify(orderRemoteGateway, times(2)).release(any());
    }

    /** 验证扫描过期订单跳过失败取消. */
    @Test
    public void testScanExpiredOrdersSkipsFailedCancel() {
        OrderPo o1 = buildOrder(1L, 1L, OrderStatusEnum.COMPLETED.intCode());
        OrderPo o2 = buildOrder(2L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.listExpiredPendingOrders(eq(OrderStatusEnum.PENDING.intCode()), eq(0L), eq(200)))
                .thenReturn(java.util.List.of(o1, o2));
        when(manager.getById(1L)).thenReturn(o1);
        when(manager.getById(2L)).thenReturn(o2);

        int count = service.scanExpiredOrders();

        assertEquals(1, count);
        assertEquals(OrderStatusEnum.COMPLETED.intCode(), o1.getStatus());
        assertEquals(OrderStatusEnum.CANCELED.intCode(), o2.getStatus());
    }

    /** 验证无过期订单时返回零. */
    @Test
    public void testScanExpiredOrdersReturnsZeroWhenEmpty() {
        when(manager.listExpiredPendingOrders(eq(OrderStatusEnum.PENDING.intCode()), eq(0L), eq(200)))
                .thenReturn(java.util.Collections.emptyList());

        assertEquals(0, service.scanExpiredOrders());
    }

    /** 验证过期订单按 ID 游标分批扫描. */
    @Test
    public void testScanExpiredOrdersScansInCursorBatches() {
        java.util.List<OrderPo> fullBatch = new java.util.ArrayList<>(200);
        for (long id = 1L; id <= 200L; id++) {
            OrderPo po = new OrderPo();
            po.setId(id);
            po.setStatus(OrderStatusEnum.PENDING.intCode());
            fullBatch.add(po);
        }
        when(manager.listExpiredPendingOrders(eq(OrderStatusEnum.PENDING.intCode()), eq(0L), eq(200)))
                .thenReturn(fullBatch);
        when(manager.listExpiredPendingOrders(eq(OrderStatusEnum.PENDING.intCode()), eq(200L), eq(200)))
                .thenReturn(java.util.Collections.emptyList());

        service.scanExpiredOrders();

        verify(manager).listExpiredPendingOrders(OrderStatusEnum.PENDING.intCode(), 0L, 200);
        verify(manager).listExpiredPendingOrders(OrderStatusEnum.PENDING.intCode(), 200L, 200);
    }

    /** 验证库存释放成功后标记为已释放. */
    @Test
    public void testScanStockReleasePendingMarksReleasedAfterSuccess() {
        OrderPo po = buildOrder(100L, 1L, OrderStatusEnum.CANCELED.intCode());
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

    /** 验证库存释放失败时保留待释放并刷新更新时间. */
    @Test
    public void testScanStockReleasePendingRefreshesFailedOrder() {
        OrderPo po = buildOrder(100L, 1L, OrderStatusEnum.CANCELED.intCode());
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

    /** 验证扫描自动确认完成已发货订单. */
    @Test
    public void testScanAutoConfirmCompletesShippedOrders() {
        OrderPo o1 = buildOrder(1L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.List.of(o1));
        when(manager.getById(1L)).thenReturn(o1);

        int count = service.scanAutoConfirm();

        assertEquals(1, count);
        assertEquals(OrderStatusEnum.COMPLETED.intCode(), o1.getStatus());
    }

    /** 验证无待自动确认订单时返回零. */
    @Test
    public void testScanAutoConfirmReturnsZeroWhenEmpty() {
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.emptyList());

        assertEquals(0, service.scanAutoConfirm());
    }

    /** 验证创建设置订单元数据. */
    @Test
    public void testCreateSetsOrderMetadata() {
        service.create(1L, 100L, 2);

        org.mockito.ArgumentCaptor<OrderPo> captor = org.mockito.ArgumentCaptor.forClass(OrderPo.class);
        verify(manager).save(captor.capture());
        OrderPo po = captor.getValue();
        assertNotNull(po.getOrderNo());
        assertEquals(0, new BigDecimal("19.80").compareTo(po.getTotalAmount()));
        assertEquals(0, new BigDecimal("19.80").compareTo(po.getPayAmount()));
        assertNotNull(po.getPayExpireAt());
    }

    /** 验证ID 服务不可用时创建订单失败. */
    @Test
    public void testCreateFailsWhenIdServiceUnavailable() {
        when(orderRemoteGateway.nextId(anyString())).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> service.create(1L, 100L, 2));

        assertEquals(OrderCodeEnum.ORDER_ID_GENERATE_FAIL.getCode(), ex.getCode());
        verify(manager, never()).save(any(OrderPo.class));
    }

    /** 验证支付时设置已支付时间并写入状态日志. */
    @Test
    public void testPaySetsPaidAtAndWritesStatusLog() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.pay(99L);

        assertNotNull(existing.getPaidAt());
        verify(statusLogManager).save(any(OrderStatusLogPo.class));
    }

    /** 验证取消订单时设置关闭时间和原因. */
    @Test
    public void testCancelSetsClosedAtAndReason() {
        OrderPo existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.cancel(99L, 1L);

        assertNotNull(existing.getClosedAt());
        assertEquals("USER_CANCEL", existing.getCloseReason());
        verify(statusLogManager).save(any(OrderStatusLogPo.class));
    }

    /** 验证结算写入条目快照. */
    @Test
    public void testCheckoutWritesItemSnapshot() {
        List<OrderCheckoutItemDto> items = List.of(new OrderCheckoutItemDto(100L, 2));
        when(orderRemoteGateway.batchSkuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                Map.of(
                                        100L,
                                        new SkuSnapshotDto(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L))));

        service.checkout(1L, items);

        org.mockito.ArgumentCaptor<OrderItemPo> captor =
                org.mockito.ArgumentCaptor.forClass(OrderItemPo.class);
        verify(orderItemManager).save(captor.capture());
        OrderItemPo item = captor.getValue();
        assertNotNull(item.getSkuSnapshotJson());
        assertNotNull(item.getSpuSnapshotJson());
        assertEquals(10L, item.getMerchantId().longValue());
        assertEquals(0, new BigDecimal("19.80").compareTo(item.getSubtotalAmount()));
    }

    /**
     * 构造订单状态测试夹具。
     *
     * <p>根据订单 ID、用户、可选商家和状态创建最小 OrderPo，并默认 SKU 100、数量 1。
     *
     * @param id 记录或订单主键，具体实体由所属方法决定
     * @param userId 用户 ID，必须对应当前订单归属
     * @param status 订单状态筛选值或状态编码
     * @return 订单测试对象。
     */
    private OrderPo buildOrder(Long id, Long userId, Integer status) {
        OrderPo po = new OrderPo();
        po.setId(id);
        po.setUserId(userId);
        po.setSkuId(100L);
        po.setQuantity(1);
        po.setStatus(status);
        return po;
    }

    /**
     * 构造订单状态测试夹具。
     *
     * <p>根据订单 ID、用户、可选商家和状态创建最小 OrderPo，并默认 SKU 100、数量 1。
     *
     * @param id 记录或订单主键，具体实体由所属方法决定
     * @param userId 用户 ID，必须对应当前订单归属
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param status 订单状态筛选值或状态编码
     * @return 订单测试对象。
     */
    private OrderPo buildOrder(Long id, Long userId, Long merchantId, Integer status) {
        OrderPo po = buildOrder(id, userId, status);
        po.setMerchantId(merchantId);
        return po;
    }

    /**
     * 构造退款回调测试 DTO。
     *
     * <p>固定支付 ID、退款请求号和金额，便于覆盖全额、部分及重复回调。
     *
     * @param orderId 订单 ID
     * @param refundedAmount 本次退款金额
     * @param totalRefundedAmount 累计退款金额
     * @param fullyRefunded 支付侧给出的是否全额退款标记
     * @return 退款回调对象。
     */
    private RefundCallbackDto refundCallback(
            Long orderId, String refundedAmount, String totalRefundedAmount, boolean fullyRefunded) {
        return new RefundCallbackDto(
                orderId,
                1L,
                new BigDecimal(refundedAmount),
                new BigDecimal(totalRefundedAmount),
                fullyRefunded,
                "REFUND-REQ-1");
    }

    /** 验证商家分页补全 SKU 名称和退款拒绝原因. */
    @Test
    public void testMerchantPageVoEnrichesSkuNameAndRejectReason() {
        OrderPo po = buildOrder(88L, 1L, 10L, OrderStatusEnum.REFUNDED.intCode());
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.singletonList(po));
        when(orderRemoteGateway.batchSkuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                java.util.Map.of(
                                        100L,
                                        new SkuSnapshotDto(
                                                100L, 1L, "iPhone 15", new BigDecimal("5999.00"), 10, 10L))));
        OrderStatusLogPo log = new OrderStatusLogPo();
        log.setOrderId(88L);
        log.setNote("证据不足");
        when(statusLogManager.list(any(Wrapper.class)))
                .thenReturn(java.util.Collections.singletonList(log));

        CursorPageVo<OrderVo> result = service.merchantPageVo(new OrderPageDto());

        OrderVo vo = result.getRecords().get(0);
        assertEquals("iPhone 15", vo.getSkuName());
        assertEquals("证据不足", vo.getRejectReason());
        assertEquals("REFUNDED", vo.getStatus());
    }

    /** 验证商家分页从订单明细补全数量和 SKU 名称. */
    @Test
    public void testMerchantPageVoFillsCheckoutQuantityAndSkuNameFromItems() {
        OrderPo po = buildOrder(90L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        po.setSkuId(null);
        po.setQuantity(null);
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.singletonList(po));

        OrderItemPo item1 = new OrderItemPo();
        item1.setOrderId(90L);
        item1.setSpuId(1L);
        item1.setSkuName("Redmi Note");
        item1.setQuantity(2);
        OrderItemPo item2 = new OrderItemPo();
        item2.setOrderId(90L);
        item2.setSpuId(2L);
        item2.setSkuName("iPhone 15");
        item2.setQuantity(3);
        OrderItemPo item3 = new OrderItemPo();
        item3.setOrderId(90L);
        item3.setSpuId(3L);
        item3.setSkuName("Pixel 9");
        item3.setQuantity(1);
        when(orderItemManager.list(any(Wrapper.class)))
                .thenReturn(java.util.List.of(item1, item2, item3));
        when(orderRemoteGateway.batchSpuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                java.util.Map.of(
                                        1L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto(1L, "小米", null),
                                        2L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto(2L, "苹果", null))));

        CursorPageVo<OrderVo> result = service.merchantPageVo(new OrderPageDto());

        OrderVo vo = result.getRecords().get(0);
        assertEquals(Integer.valueOf(6), vo.getQuantity());
        assertEquals("小米 | Redmi Note * 2、苹果 | iPhone 15 * 3…", vo.getSkuName());
    }

    /** 验证商家详情返回补全后的订单视图. */
    @Test
    public void testMerchantDetailVoReturnsEnrichedOrder() {
        OrderPo po = buildOrder(91L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        po.setSkuId(null);
        po.setQuantity(null);
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);

        OrderItemPo item = new OrderItemPo();
        item.setOrderId(91L);
        item.setSpuId(1L);
        item.setSkuName("Redmi Note");
        item.setQuantity(2);
        when(orderItemManager.list(any(Wrapper.class)))
                .thenReturn(java.util.Collections.singletonList(item));
        when(orderRemoteGateway.batchSpuSnapshot(any()))
                .thenReturn(
                        Result.success(
                                java.util.Map.of(
                                        1L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto(1L, "小米", null))));

        OrderVo vo = service.merchantDetailVo(91L, 10L);

        assertEquals(Integer.valueOf(2), vo.getQuantity());
        assertEquals("小米 | Redmi Note * 2", vo.getSkuName());
    }

    /** 验证商家详情在订单不存在时抛出业务异常. */
    @Test
    public void testMerchantDetailVoThrowsWhenOrderNotFound() {
        when(manager.getOne(any(Wrapper.class))).thenReturn(null);

        assertThrows(BizException.class, () -> service.merchantDetailVo(99L, 10L));
    }
}
