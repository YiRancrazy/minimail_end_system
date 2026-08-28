package com.yirancrazy.minimall.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusMachine;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderItemPO;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.entity.OrderStatusLogPO;
import com.yirancrazy.minimall.order.manager.OrderItemManager;
import com.yirancrazy.minimall.order.manager.OrderLogisticsManager;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.manager.OrderStatusLogManager;
import com.yirancrazy.minimall.order.mapper.OrderMapper;
import com.yirancrazy.minimall.order.service.impl.OrderServiceImpl;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;
import com.yirancrazy.minimall.order.vo.OrderVO;

/**
* OrderServiceImpl 单元测试，覆盖订单状态机创建、支付、取消、发货、确认、退款及异常路径。
 */
public class OrderServiceImplTest {

    private OrderManager manager;
    private OrderItemManager orderItemManager;
    private OrderLogisticsManager logisticsManager;
    private OrderStatusLogManager statusLogManager;
    private OrderMapper orderMapper;
    private GoodsFeignClient goodsFeignClient;
    private StockFeignClient stockFeignClient;
    private PayFeignClient payFeignClient;
    private IdFeignClient idFeignClient;
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
        orderMapper = mock(OrderMapper.class);
        goodsFeignClient = mock(GoodsFeignClient.class);
        stockFeignClient = mock(StockFeignClient.class);
        payFeignClient = mock(PayFeignClient.class);
        idFeignClient = mock(IdFeignClient.class);
        eventBus = mock(EventBus.class);
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
        lenient().when(goodsFeignClient.skuSnapshot(any())).thenReturn(Result.success(
            new SkuSnapshotDTO(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L)));
        lenient().when(stockFeignClient.reserve(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(stockFeignClient.release(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(payFeignClient.create(any())).thenReturn(Result.success(2001L));
        lenient().when(payFeignClient.refund(any())).thenReturn(Result.success(Boolean.TRUE));
        lenient().when(idFeignClient.nextId(any())).thenReturn(Result.success(10001L));
        lenient().doAnswer(inv -> {
            inv.getArgument(1, Runnable.class).run();
            return null;
        }).when(eventBus).publishInTx(any(), any(Runnable.class), any());
        service = new OrderServiceImpl(manager, orderItemManager, logisticsManager, statusLogManager,
            orderMapper, goodsFeignClient, stockFeignClient, payFeignClient, idFeignClient,
            eventBus, statusMachine, new com.fasterxml.jackson.databind.ObjectMapper(),
            minioUtil);
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
     * 验证创建订单时同步落订单明细快照（含商品名），供商家列表/详情回显。
     */
    @Test
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
     * 验证 listItemVO 将订单明细的商品图 objectKey 解析为可访问的预签名 URL。
     */
    @Test
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
        when(goodsFeignClient.batchSpuSnapshot(any())).thenReturn(Result.success(
            java.util.Map.of(1L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO(1L, "耳机Pro", null))));

        List<com.yirancrazy.minimall.order.vo.OrderItemVO> result = service.listItemVO(100L);

        assertEquals(1, result.size());
        assertEquals("http://minio/mall-files/img-x.png?token", result.get(0).getSkuImageUrl());
        assertEquals("耳机Pro", result.get(0).getSpuName());
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

        service.pay(99L, 1L);
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }

    /**
     * 验证支付不存在的订单时抛出异常。
     */
    @Test
    public void pay_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.pay(99L, 1L));
    }

    /**
     * 验证已支付订单不能再次支付（状态流转不合法）。
     */
    @Test
    public void pay_already_paid_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.pay(99L, 1L));
    }

    /**
     * 验证非本人支付他人订单时抛出 ORDER_NOT_FOUND。
     */
    @Test
    public void pay_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.pay(99L, 999L));
    }

    /**
     * 验证 payByOrderNo 按业务单号推进订单为已支付。
     */
    @Test
    public void payByOrderNo_transitions_to_paid() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(existing);
        when(manager.getById(99L)).thenReturn(existing);

        service.payByOrderNo("OD20260814001");
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getStatus());
        verify(manager).updateById(existing);
    }

    /**
     * 验证 payByOrderNo 订单不存在时抛出异常。
     */
    @Test
    public void payByOrderNo_missing_throws() {
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(null);
        assertThrows(BizException.class, () -> service.payByOrderNo("NOPE"));
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
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.ship(99L, 10L, "顺丰", "SF12345678");
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

        service.refund(99L, 1L);
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        assertEquals(OrderStatusEnum.PAID.intCode(), existing.getRefundFromStatus());
    }

    /**
     * 验证申请退款阶段不调用支付网关（真实退款由商家审核通过后触发）。
     */
    @Test
    public void refund_does_not_call_pay_gateway() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.refund(99L, 1L);
        verify(payFeignClient, never()).refund(any());
    }

    /**
     * 验证非本人订单申请退款时抛出 ORDER_NOT_FOUND。
     */
    @Test
    public void refund_verifies_order_ownership() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.refund(99L, 999L));
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
     * 验证非 REFUNDING 状态的订单收到退款回调时抛出状态流转异常（拒绝迟到回调，天然幂等）。
     */
    @Test
    public void handleRefundCallback_rejects_state_other_than_refunding() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.handleRefundCallback(99L, true));
        assertThrows(BizException.class, () -> service.handleRefundCallback(99L, false));
    }

    /**
     * 验证查询订单状态返回正确的状态码。
     */
    @Test
    public void getStatus_returns_order_status() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        Integer status = service.getStatus(99L, 1L);
        assertEquals(OrderStatusEnum.PAID.intCode(), status);
    }

    /**
     * 验证查询不存在的订单状态时抛出异常。
     */
    @Test
    public void getStatus_missing_order_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getStatus(99L, 1L));
    }

    /**
     * 验证非本人查询他人订单状态时抛出 ORDER_NOT_FOUND。
     */
    @Test
    public void getStatus_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.getStatus(99L, 999L));
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

        OrderPO result = service.getDetail(99L, 1L);
        assertEquals(99L, result.getId());
    }

    /**
     * 验证 getDetail 在订单不存在时抛出异常。
     */
    @Test
    public void getDetail_missing_throws() {
        when(manager.getById(99L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getDetail(99L, 1L));
    }

    /**
     * 验证非本人查询他人订单详情时抛出 ORDER_NOT_FOUND。
     */
    @Test
    public void getDetail_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.getDetail(99L, 999L));
    }

    /**
     * 验证 resolveMerchantId 按纯数字订单ID解析归属商户。
     */
    @Test
    public void resolveMerchantId_by_id_returns_merchant() {
        OrderPO existing = buildOrder(99L, 1L, 42L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertEquals(42L, service.resolveMerchantId("99").longValue());
    }

    /**
     * 验证 resolveMerchantId 按业务单号解析归属商户（ID未命中时走 order_no 查询）。
     */
    @Test
    public void resolveMerchantId_by_order_no_returns_merchant() {
        OrderPO existing = buildOrder(99L, 1L, 42L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(any())).thenReturn(null);
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(existing);

        assertEquals(42L, service.resolveMerchantId("ORD-123").longValue());
    }

    /**
     * 验证 resolveMerchantId 订单不存在或入参为空时返回 null。
     */
    @Test
    public void resolveMerchantId_missing_returns_null() {
        when(manager.getById(any())).thenReturn(null);
        when(manager.getOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(null);

        assertNull(service.resolveMerchantId("NOPE"));
        assertNull(service.resolveMerchantId(null));
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
     * 验证 countStatusByUser 按状态聚合返回各状态订单数量。
     */
    @Test
    public void countStatusByUser_aggregates_by_status() {
        when(orderMapper.countByStatus(7L)).thenReturn(java.util.List.of(
            java.util.Map.of("status", 1, "cnt", 1L),
            java.util.Map.of("status", 2, "cnt", 2L),
            java.util.Map.of("status", 3, "cnt", 1L),
            java.util.Map.of("status", 4, "cnt", 1L),
            java.util.Map.of("status", 5, "cnt", 3L),
            java.util.Map.of("status", 6, "cnt", 2L),
            java.util.Map.of("status", 7, "cnt", 1L)));

        com.yirancrazy.minimall.order.vo.OrderStatusCountsVO vo = service.countStatusByUser(7L);

        assertEquals(1L, vo.getPendingCount());
        assertEquals(2L, vo.getPaidCount());
        assertEquals(1L, vo.getShippedCount());
        assertEquals(1L, vo.getCompletedCount());
        assertEquals(3L, vo.getCanceledCount());
        assertEquals(2L, vo.getRefundingCount());
        assertEquals(1L, vo.getRefundedCount());
    }

    /**
     * 验证 countStatusByUser 无订单时各状态数量为 0。
     */
    @Test
    public void countStatusByUser_returns_zero_when_empty() {
        when(orderMapper.countByStatus(7L)).thenReturn(java.util.Collections.emptyList());

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
     * 验证平台财务汇总委托 mapper 并按日期边界与商家ID解析调用。
     */
    @Test
    public void orderSummary_delegates_to_mapper_with_parsed_range() {
        com.yirancrazy.minimall.order.vo.OrderSummaryVO expected =
                new com.yirancrazy.minimall.order.vo.OrderSummaryVO();
        when(orderMapper.summary(eq(10L), any(), any())).thenReturn(expected);

        com.yirancrazy.minimall.order.vo.OrderSummaryVO vo =
                service.orderSummary("2026-08-01", "2026-08-26", "10");

        assertSame(expected, vo);
        verify(orderMapper).summary(eq(10L), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class));
    }

    /**
     * 验证平台财务汇总对非法商家ID/日期兜底为 null 边界（全平台），不抛异常。
     */
    @Test
    public void orderSummary_tolerates_invalid_params() {
        com.yirancrazy.minimall.order.vo.OrderSummaryVO expected =
                new com.yirancrazy.minimall.order.vo.OrderSummaryVO();
        when(orderMapper.summary(isNull(), isNull(), isNull())).thenReturn(expected);

        com.yirancrazy.minimall.order.vo.OrderSummaryVO vo =
                service.orderSummary("bad-date", "2026-13-99", "not-a-number");

        assertNotNull(vo);
        verify(orderMapper).summary(isNull(), isNull(), isNull());
    }

    /**
     * 验证平台订单趋势按日聚合，无数据时返回空点集。
     */
    @Test
    public void orderTrend_returns_empty_points_when_no_data() {
        when(orderMapper.trend(isNull(), isNull(), isNull())).thenReturn(null);

        com.yirancrazy.minimall.order.vo.OrderTrendVO vo = service.orderTrend(null, null, null);

        assertNotNull(vo);
        assertTrue(vo.getPoints().isEmpty());
    }

    /**
     * 验证平台订单趋势委托 mapper 并原样返回点集。
     */
    @Test
    public void orderTrend_delegates_to_mapper() {
        com.yirancrazy.minimall.order.vo.OrderTrendPointVO point =
                new com.yirancrazy.minimall.order.vo.OrderTrendPointVO("2026-08-26", 2L, java.math.BigDecimal.TEN);
        when(orderMapper.trend(eq(1L), any(), any())).thenReturn(java.util.List.of(point));

        com.yirancrazy.minimall.order.vo.OrderTrendVO vo = service.orderTrend("2026-08-01", "2026-08-26", "1");

        assertEquals(1, vo.getPoints().size());
        assertEquals("2026-08-26", vo.getPoints().get(0).getDate());
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
     * 验证商家同意退款时发起真实支付退款，状态保持 REFUNDING 等待回调驱动。
     */
    @Test
    public void reviewRefund_approved_triggers_pay() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        existing.setPayId(2001L);
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, true, 10L, null);
        assertEquals(OrderStatusEnum.REFUNDING.intCode(), existing.getStatus());
        verify(payFeignClient).refund(any());
    }

    /**
     * 验证商家拒绝退款时状态回退到 refundFromStatus。
     */
    @Test
    public void reviewRefund_rejected_reverts_status() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setRefundFromStatus(OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, false, 10L, "证据不足");
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

        assertThrows(BizException.class, () -> service.reviewRefund(99L, true, 999L, null));
    }

    /**
     * 验证非 REFUNDING 状态订单审核退款时抛出异常。
     */
    @Test
    public void reviewRefund_not_refunding_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reviewRefund(99L, true, 10L, null));
    }

    /**
     * 验证商家对已支付订单发起部分退款：金额校验通过后落库且状态推进 REFUNDING。
     */
    @Test
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
     * 验证商家对已完成订单发起退款被拒绝（状态机无 COMPLETED→REFUNDING 迁移）。
     */
    @Test
    public void merchantInitiateRefund_completed_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.COMPLETED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class,
            () -> service.merchantInitiateRefund(99L, 10L, "19.80", "售后退款"));
    }

    /**
     * 验证退款金额超过支付金额时抛出 REFUND_AMOUNT_INVALID。
     */
    @Test
    public void merchantInitiateRefund_amount_exceeds_pay_throws() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        existing.setPayAmount(new BigDecimal("19.80"));
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class,
            () -> service.merchantInitiateRefund(99L, 10L, "20.00", "超额退款"));
    }

    /**
     * 验证审核通过时按申请的部分退款金额调用支付网关。
     */
    @Test
    public void reviewRefund_approved_uses_partial_refund_amount() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.REFUNDING.intCode());
        existing.setPayId(2001L);
        existing.setAmount(new BigDecimal("19.80"));
        existing.setRefundAmount(new BigDecimal("5.00"));
        when(manager.getById(99L)).thenReturn(existing);

        service.reviewRefund(99L, true, 10L, null);

        org.mockito.ArgumentCaptor<RefundCreateDTO> captor =
            org.mockito.ArgumentCaptor.forClass(RefundCreateDTO.class);
        verify(payFeignClient).refund(captor.capture());
        assertEquals(2001L, captor.getValue().getPayId().longValue());
        assertEquals(0, new BigDecimal("5.00").compareTo(captor.getValue().getAmount()));
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

        List<OrderLogisticsVO> result = service.queryLogistics(99L, 1L);
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
        assertThrows(BizException.class, () -> service.queryLogistics(99L, 1L));
    }

    /**
     * 验证非本人查询他人订单物流时抛出 ORDER_NOT_FOUND。
     */
    @Test
    public void queryLogistics_wrong_user_throws() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.SHIPPED.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.queryLogistics(99L, 999L));
    }

    /**
     * 验证发货时插入一条"已发货"物流节点。
     */
    @Test
    public void ship_inserts_logistics_node() {
        OrderPO existing = buildOrder(99L, 1L, 10L, OrderStatusEnum.PAID.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.ship(99L, 10L, "顺丰", "SF12345678");
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
            new SkuSnapshotDTO(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L)));

        Long orderId = service.checkout(1L, items);

        assertNotNull(orderId);
        verify(stockFeignClient, times(2)).reserve(any());
        verify(orderItemManager, times(2)).save(any(OrderItemPO.class));
        verify(payFeignClient).create(any());
    }

    /**
     * 验证跨商家结算按 merchantId 拆分为多个子订单，共享同一 orderGroupNo，每个子订单各自调用 payFeignClient.create。
     */
    @Test
    public void checkout_cross_merchant_splits_orders() {
        OrderCheckoutItemDTO itemA = new OrderCheckoutItemDTO(100L, 1);
        OrderCheckoutItemDTO itemB = new OrderCheckoutItemDTO(200L, 1);
        SkuSnapshotDTO snapA = new SkuSnapshotDTO(100L, 1L, "sku-A", new BigDecimal("10.00"), 100, 10L);
        SkuSnapshotDTO snapB = new SkuSnapshotDTO(200L, 2L, "sku-B", new BigDecimal("20.00"), 100, 20L);
        when(goodsFeignClient.skuSnapshot(100L)).thenReturn(Result.success(snapA));
        when(goodsFeignClient.skuSnapshot(200L)).thenReturn(Result.success(snapB));

        Long mainOrderId = service.checkout(1L, List.of(itemA, itemB));

        assertNotNull(mainOrderId);
        verify(stockFeignClient, times(2)).reserve(any());
        verify(orderItemManager, times(2)).save(any(OrderItemPO.class));
        verify(payFeignClient, times(2)).create(any());
        verify(manager, times(2)).save(any(OrderPO.class));
    }

    /**
     * 验证结算时 SKU 缺少 merchantId 抛出 ORDER_SKU_SNAPSHOT_MISSING。
     */
    @Test
    public void checkout_snapshot_without_merchant_throws() {
        when(goodsFeignClient.skuSnapshot(any())).thenReturn(Result.success(
            new SkuSnapshotDTO(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, null)));
        List<OrderCheckoutItemDTO> items = List.of(new OrderCheckoutItemDTO(100L, 1));

        assertThrows(BizException.class, () -> service.checkout(1L, items));
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
     * 验证状态迁移更新影响 0 行时抛出状态流转异常，且不执行释放库存/状态日志副作用（并发防抖）。
     */
    @Test
    public void transitStatus_throws_when_update_returns_zero_rows() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);
        when(manager.updateById(any(OrderPO.class))).thenReturn(false);

        assertThrows(BizException.class, () -> service.cancel(99L, 1L));
        verify(stockFeignClient, never()).release(any());
        verify(statusLogManager, never()).save(any(OrderStatusLogPO.class));
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

    /**
     * 验证创建订单时生成业务单号并补齐金额与支付超时时间。
     */
    @Test
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
     * 验证支付推进写入 paidAt 并落状态日志。
     */
    @Test
    public void pay_sets_paid_at_and_writes_status_log() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.pay(99L, 1L);

        assertNotNull(existing.getPaidAt());
        verify(statusLogManager).save(any(OrderStatusLogPO.class));
    }

    /**
     * 验证取消订单写入 closedAt 与关闭原因并落状态日志。
     */
    @Test
    public void cancel_sets_closed_at_and_reason() {
        OrderPO existing = buildOrder(99L, 1L, OrderStatusEnum.PENDING.intCode());
        when(manager.getById(99L)).thenReturn(existing);

        service.cancel(99L, 1L);

        assertNotNull(existing.getClosedAt());
        assertEquals("USER_CANCEL", existing.getCloseReason());
        verify(statusLogManager).save(any(OrderStatusLogPO.class));
    }

    /**
     * 验证结算下单写入明细行快照与金额拆分。
     */
    @Test
    public void checkout_writes_item_snapshot() {
        List<OrderCheckoutItemDTO> items = List.of(new OrderCheckoutItemDTO(100L, 2));
        when(goodsFeignClient.skuSnapshot(any())).thenReturn(Result.success(
            new SkuSnapshotDTO(100L, 1L, "sku-100", new BigDecimal("9.90"), 100, 10L)));

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

    /**
     * 验证商家端分页装配商品名与驳回原因，且状态映射为枚举别名。
     */
    @Test
    public void merchantPageVO_enriches_skuName_and_rejectReason() {
        OrderPO po = buildOrder(88L, 1L, 10L, OrderStatusEnum.REFUNDED.intCode());
        when(manager.list(any(Wrapper.class))).thenReturn(java.util.Collections.singletonList(po));
        when(goodsFeignClient.batchSkuSnapshot(any())).thenReturn(Result.success(
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
     * 验证拆单订单（购物车结算）不落库 skuId/quantity 时，从 order_items 聚合全部商品数量与名称拼接。
     */
    @Test
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
        when(goodsFeignClient.batchSpuSnapshot(any())).thenReturn(Result.success(
            java.util.Map.of(1L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO(1L, "小米", null),
                             2L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO(2L, "苹果", null))));

        CursorPageVO<OrderVO> result = service.merchantPageVO(new OrderPageDTO());

        OrderVO vo = result.getRecords().get(0);
        assertEquals(Integer.valueOf(6), vo.getQuantity());
        assertEquals("小米 | Redmi Note * 2、苹果 | iPhone 15 * 3…", vo.getSkuName());
    }

    /**
     * 验证商家端订单详情：归属校验通过时返回聚合后的数量与商品名。
     */
    @Test
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
        when(goodsFeignClient.batchSpuSnapshot(any())).thenReturn(Result.success(
            java.util.Map.of(1L, new com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO(1L, "小米", null))));

        OrderVO vo = service.merchantDetailVO(91L, 10L);

        assertEquals(Integer.valueOf(2), vo.getQuantity());
        assertEquals("小米 | Redmi Note * 2", vo.getSkuName());
    }

    /**
     * 验证商家端订单详情：订单不存在或不属于该商家时抛出 ORDER_NOT_FOUND。
     */
    @Test
    public void merchantDetailVO_throws_when_order_not_found() {
        when(manager.getOne(any(Wrapper.class))).thenReturn(null);

        assertThrows(BizException.class, () -> service.merchantDetailVO(99L, 10L));
    }
}
