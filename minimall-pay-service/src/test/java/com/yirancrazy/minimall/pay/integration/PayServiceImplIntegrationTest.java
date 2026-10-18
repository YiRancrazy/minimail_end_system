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
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
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
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayServiceImplIntegrationTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
     * @Author: yirancrazy@gmail.com
     * @Description: TestMetaObjectConfig 配置类。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    @org.springframework.boot.test.context.TestConfiguration
    static class TestMetaObjectConfig {

        /**
         * 创建元对象处理器。
         * @return 元对象处理器
         */
        @org.springframework.context.annotation.Bean
        com.baomidou.mybatisplus.core.handlers.MetaObjectHandler metaObjectHandler() {
            return new com.baomidou.mybatisplus.core.handlers.MetaObjectHandler() {
                public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                    this.strictInsertFill(metaObject, "createTime", java.time.LocalDateTime.class,
                        java.time.LocalDateTime.now());
                    this.strictInsertFill(metaObject, "updateTime", java.time.LocalDateTime.class,
                        java.time.LocalDateTime.now());
                    this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
                    this.strictInsertFill(metaObject, "version", Integer.class, 0);
                }

                /**
                 * 更新业务数据。
                 * @param metaObject metaObject 参数
                 */
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
            if ("ORD-INT-STATS".equals(orderNo)) {
                return Result.success(new OrderPaySnapshotDTO(
                    6L, orderNo, 100L, 77777L, new BigDecimal("100.00"), 1, null));
            }
            return Result.success(null);
        });
    }


    @Test
    void createPayment_persists_and_getByOrderNo_returns_it() {
        Long payId = payService.createPayment("ORD-INT-1", 100L, 200L, new BigDecimal("99.99"), null);
        assertNotNull(payId);

        com.yirancrazy.minimall.pay.vo.PayTransactionVO vo =
            payService.getStatusVO("ORD-INT-1", 100L);
        assertNotNull(vo);
        assertEquals(1, vo.getStatus());
        assertEquals(0, new BigDecimal("99.99").compareTo(vo.getAmount()));
    }


    @Test
    void getByOrderNo_rejects_other_user() {
        payService.createPayment("ORD-INT-OWNER", 100L, 200L, new BigDecimal("19.99"), null);

        assertThrows(BizException.class, () -> payService.getStatusVO("ORD-INT-OWNER", 999L));
    }


    @Test
    void createPayment_unsupported_channel_throws() {
        assertThrows(BizException.class,
            () -> payService.createPayment("ORD-INT-2", 100L, 200L, new BigDecimal("10.00"), 2));
    }


    @Test
    void getPaymentParams_returns_persisted_fields() {
        payService.createPayment("ORD-INT-3", 1L, 2L, new BigDecimal("50.00"), null);
        com.yirancrazy.minimall.pay.vo.PayTransactionVO po =
            payService.getStatusVO("ORD-INT-3", 1L);

        com.yirancrazy.minimall.pay.vo.PaymentParamsVO params =
            payService.getPaymentParams(po.getPaymentNo(), 1L);
        assertNotNull(params);
        assertEquals(po.getPaymentNo(), params.getPaymentNo());
        assertEquals(0, new BigDecimal("50.00").compareTo(params.getAmount()));
        assertEquals("CNY", params.getCurrency());
    }


    @Test
    void getByOrderNo_missing_throws_biz() {
        assertThrows(BizException.class, () -> payService.getStatusVO("ORD-INT-NOT-EXIST", 100L));
    }


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
        lenient().when(orderFeignClient.refundCallback(any(RefundCallbackDTO.class)))
            .thenReturn(Result.success(null));

        assertEquals(1, payService.scanPendingRefunds());
        assertEquals(1, payRefundMapper.findByRefundRequestNo("REFUND-INT-REQ").getStatus());
        assertEquals(0, payService.scanPendingRefunds());
        verify(alipayGateway, times(1)).queryRefund(payTx.getPaymentNo(), "REFUND-INT-PENDING");
        verify(alipayGateway, never()).refund(
            anyString(), anyString(), any(BigDecimal.class), anyString());
    }


    @Test
    void statistics_includesMultiplePartialRefunds() {
        Long payId = payService.createPayment(
            "ORD-INT-STATS", 100L, 77777L, new BigDecimal("100.00"), null);
        PayTransactionPO payTx = payManager.getById(payId);
        payTx.setStatus(2);
        payManager.updateById(payTx);

        payRefundMapper.insert(successfulRefund("REFUND-STATS-1", "REQ-STATS-1",
            payTx.getPaymentNo(), "30.00"));
        payRefundMapper.insert(successfulRefund("REFUND-STATS-2", "REQ-STATS-2",
            payTx.getPaymentNo(), "20.00"));

        com.yirancrazy.minimall.pay.dto.PayStatisticsDTO statistics =
            payTransactionMapper.statistics(77777L, null, null);

        assertEquals(0, new BigDecimal("100.00").compareTo(statistics.getTotalAmount()));
        assertEquals(0, new BigDecimal("50.00").compareTo(statistics.getRefundAmount()));
    }

    private PayRefundPO successfulRefund(String refundNo, String requestNo,
                                         String paymentNo, String amount) {
        PayRefundPO refund = new PayRefundPO();
        refund.setRefundNo(refundNo);
        refund.setRefundRequestNo(requestNo);
        refund.setPaymentNo(paymentNo);
        refund.setAmount(new BigDecimal(amount));
        refund.setStatus(1);
        return refund;
    }
}
