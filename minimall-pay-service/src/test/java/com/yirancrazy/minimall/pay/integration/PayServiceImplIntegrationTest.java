package com.yirancrazy.minimall.pay.integration;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;
import com.yirancrazy.minimall.pay.service.PayService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayService 集成测试，使用 H2 内存库 + Mock 外部网关 / Feign。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@SpringBootTest
@ActiveProfiles("test")
class PayServiceImplIntegrationTest {

    private static final AtomicLong NEXT_ID = new AtomicLong(80001L);

    @Autowired
    private PayService payService;

    @MockBean
    private AlipayGateway alipayGateway;

    @MockBean
    private OrderFeignClient orderFeignClient;

    @MockBean
    private IdFeignClient idFeignClient;

    @SpyBean
    private PayTransactionMapper payTransactionMapper;

    @Autowired
    private PayRefundMapper payRefundMapper;

    @Autowired
    private PayManager payManager;

    /**
     * 测试上下文：注册 MetaObjectHandler 解决 BasePO.isDeleted 自动填充。
     * 默认 service.save 不会显式 setIsDeleted，需要在 INSERT 前填 0。
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class TestMetaObjectConfig {

        @org.springframework.context.annotation.Bean
        com.baomidou.mybatisplus.core.handlers.MetaObjectHandler metaObjectHandler() {
            return new com.baomidou.mybatisplus.core.handlers.MetaObjectHandler() {
                @Override
                public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                    this.strictInsertFill(metaObject, "createTime", java.time.LocalDateTime.class,
                        java.time.LocalDateTime.now());
                    this.strictInsertFill(metaObject, "updateTime", java.time.LocalDateTime.class,
                        java.time.LocalDateTime.now());
                    this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
                    this.strictInsertFill(metaObject, "version", Integer.class, 0);
                }

                @Override
                public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                    this.strictUpdateFill(metaObject, "updateTime", java.time.LocalDateTime.class,
                        java.time.LocalDateTime.now());
                }
            };
        }
    }

    @BeforeEach
    void setUp() {
        lenient().when(idFeignClient.nextId(anyString()))
            .thenAnswer(invocation -> Result.success(NEXT_ID.getAndIncrement()));
        lenient().when(alipayGateway.createPagePayment(anyString(), any(), anyString(), anyString()))
            .thenReturn("<form action=\"http://pay.url/test\"></form>");
        lenient().when(alipayGateway.refund(anyString(), anyString(), any(), anyString()))
            .thenReturn("REFUND_TRADE_TEST");
        lenient().when(orderFeignClient.paySnapshot(anyString())).thenAnswer(invocation -> {
            String orderNo = invocation.getArgument(0);
            if ("ORD-INT-1".equals(orderNo)) {
                return Result.success(new OrderPaySnapshotDTO(
                    1L, orderNo, 100L, 200L, new BigDecimal("99.99"), 1, null));
            }
            if ("ORD-INT-OWNER".equals(orderNo)) {
                return Result.success(new OrderPaySnapshotDTO(
                    2L, orderNo, 100L, 200L, new BigDecimal("19.99"), 1, null));
            }
            if ("ORD-INT-3".equals(orderNo)) {
                return Result.success(new OrderPaySnapshotDTO(
                    3L, orderNo, 1L, 2L, new BigDecimal("50.00"), 1, null));
            }
            if ("ORD-INT-ROLLBACK".equals(orderNo)) {
                return Result.success(new OrderPaySnapshotDTO(
                    4L, orderNo, 100L, 200L, new BigDecimal("10.00"), 1, null));
            }
            if ("ORD-INT-SCAN".equals(orderNo)) {
                return Result.success(new OrderPaySnapshotDTO(
                    5L, orderNo, 100L, 200L, new BigDecimal("10.00"), 1, null));
            }
            return Result.success(null);
        });
    }

    /**
     * 端到端：createPayment 写入 PENDING 行后 getByOrderNo 可见。
     */
    @Test
    void createPayment_persists_and_getByOrderNo_returns_it() {
        Long payId = payService.createPayment("ORD-INT-1", 100L, 200L, new BigDecimal("99.99"), null);
        assertNotNull(payId);

        PayTransactionPO po = payService.getByOrderNo("ORD-INT-1", 100L);
        assertNotNull(po);
        assertEquals(1, po.getStatus());
        assertEquals(0, new BigDecimal("99.99").compareTo(po.getAmount()));
    }

    /**
     * 端到端：getByOrderNo 查询他人支付流水时按不存在处理。
     */
    @Test
    void getByOrderNo_rejects_other_user() {
        payService.createPayment("ORD-INT-OWNER", 100L, 200L, new BigDecimal("19.99"), null);

        assertThrows(BizException.class, () -> payService.getByOrderNo("ORD-INT-OWNER", 999L));
    }

    /**
     * 端到端：createPayment 显式 channel=2（WECHAT）应抛 PAY_CHANNEL_UNSUPPORTED。
     */
    @Test
    void createPayment_unsupported_channel_throws() {
        assertThrows(BizException.class,
            () -> payService.createPayment("ORD-INT-2", 100L, 200L, new BigDecimal("10.00"), 2));
    }

    /**
     * 端到端：getPaymentParams 返回的 VO 字段与持久化一致。
     */
    @Test
    void getPaymentParams_returns_persisted_fields() {
        payService.createPayment("ORD-INT-3", 1L, 2L, new BigDecimal("50.00"), null);
        PayTransactionPO po = payService.getByOrderNo("ORD-INT-3", 1L);

        com.yirancrazy.minimall.pay.vo.PaymentParamsVO params =
            payService.getPaymentParams(po.getPaymentNo(), 1L);
        assertNotNull(params);
        assertEquals(po.getPaymentNo(), params.getPaymentNo());
        assertEquals(0, new BigDecimal("50.00").compareTo(params.getAmount()));
        assertEquals("CNY", params.getCurrency());
    }

    /**
     * 端到端：getByOrderNo 不存在时抛 PAY_NOT_FOUND。
     */
    @Test
    void getByOrderNo_missing_throws_biz() {
        assertThrows(BizException.class, () -> payService.getByOrderNo("ORD-INT-NOT-EXIST", 100L));
    }

    /**
     * 端到端：支付单抢占抛异常时，已插入的 PENDING 退款和支付状态在同一事务内回滚。
     */
    @Test
    void createRefund_rollsBackPendingWhenStatusClaimFails() {
        Long payId = payService.createPayment(
            "ORD-INT-ROLLBACK", 100L, 200L, new BigDecimal("10.00"), null);
        PayTransactionPO payTx = payManager.getById(payId);
        payTx.setStatus(2);
        payManager.updateById(payTx);
        doThrow(new IllegalStateException("status claim failed"))
            .when(payTransactionMapper).updateStatusIf(payTx.getPaymentNo(), 2, 5);

        assertThrows(IllegalStateException.class, () -> payService.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                payId, new BigDecimal("10.00"), "rollback", "REFUND-ROLLBACK")));

        assertNull(payRefundMapper.findByRefundRequestNo("REFUND-ROLLBACK"));
        assertEquals(2, payManager.getById(payId).getStatus());
    }

    /**
     * 端到端：PENDING 退款经渠道查询补成 SUCCESS，重复扫描不会再次调用渠道。
     */
    @Test
    void scanPendingRefunds_reconcilesExternalSuccessOnce() {
        Long payId = payService.createPayment(
            "ORD-INT-SCAN", 100L, 200L, new BigDecimal("10.00"), null);
        PayTransactionPO payTx = payManager.getById(payId);
        payTx.setStatus(5);
        payManager.updateById(payTx);

        PayRefundPO refund = new PayRefundPO();
        refund.setRefundNo("REFUND-INT-PENDING");
        refund.setRefundRequestNo("REFUND-INT-REQ");
        refund.setPaymentNo(payTx.getPaymentNo());
        refund.setAmount(new BigDecimal("10.00"));
        refund.setStatus(0);
        refund.setRetryCount(0);
        refund.setNextRetryAt(java.time.LocalDateTime.now().minusSeconds(1));
        payRefundMapper.insert(refund);
        lenient().when(alipayGateway.queryRefund(payTx.getPaymentNo(), "REFUND-INT-PENDING"))
            .thenReturn(com.yirancrazy.minimall.pay.gateway.PayGateway.RefundQueryStatus.SUCCESS);
        lenient().when(orderFeignClient.refundCallback(5L, true)).thenReturn(Result.success(null));

        assertEquals(1, payService.scanPendingRefunds());
        assertEquals(1, payRefundMapper.findByRefundRequestNo("REFUND-INT-REQ").getStatus());
        assertEquals(0, payService.scanPendingRefunds());
        verify(alipayGateway, times(1)).queryRefund(payTx.getPaymentNo(), "REFUND-INT-PENDING");
        verify(alipayGateway, never()).refund(
            anyString(), anyString(), any(BigDecimal.class), anyString());
    }
}
