package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.constant.PayCodeEnum;
import com.yirancrazy.minimall.pay.dto.AlipayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementResultDTO;
import com.yirancrazy.minimall.pay.dto.PayStatisticsDTO;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.PayGateway;
import com.yirancrazy.minimall.pay.gateway.PayRemoteGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.manager.PayRefundManager;
import com.yirancrazy.minimall.pay.service.impl.PayServiceImpl;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class PayServiceImplTest {

    private PayManager manager;
    private PayRefundManager payRefundManager;
    private PayGateway payGateway;
    private PayRemoteGateway payRemoteGateway;
    private PlatformTransactionManager transactionManager;
    private PayServiceImpl service;

    @BeforeAll
    static void initLambdaCache() {
        // 无 MyBatis 容器时 LambdaQueryWrapper 列名解析/参数绑定懒执行到 getSqlSegment 才触发，
        // 需预置 TableInfo 的 lambda 缓存，否则 wrapper.getSqlSegment() 抛 "can not find lambda cache"
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PayTransactionPO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), PayRefundPO.class);
    }

    @BeforeEach
    void setUp() {
        manager = mock(PayManager.class);
        payRefundManager = mock(PayRefundManager.class);
        payGateway = mock(PayGateway.class);
        payRemoteGateway = mock(PayRemoteGateway.class);
        transactionManager = mock(PlatformTransactionManager.class);
        lenient().when(transactionManager.getTransaction(any()))
            .thenReturn(new SimpleTransactionStatus());
        lenient().when(payRemoteGateway.nextId(anyString())).thenReturn(Result.success(90001L));
        lenient().when(payRemoteGateway.paySnapshot(anyString())).thenAnswer(invocation -> {
            String orderNo = invocation.getArgument(0);
            if ("ORDER100".equals(orderNo)) {
                return Result.success(snapshot(orderNo, 1L, 1L, "99.99", 1));
            }
            if ("ORDER200".equals(orderNo)) {
                return Result.success(snapshot(orderNo, 2L, 3L, "50.00", 1));
            }
            if ("ORDER300".equals(orderNo)) {
                return Result.success(snapshot(orderNo, 3L, null, "10.00", 1));
            }
            if ("ORDER400".equals(orderNo)) {
                return Result.success(snapshot(orderNo, 4L, 42L, "10.00", 1));
            }
            return Result.success(null);
        });
        lenient().when(payRemoteGateway.pay(anyLong())).thenReturn(Result.success(null));
        lenient().when(payRemoteGateway.payByOrderNo(anyString())).thenReturn(Result.success(null));
        lenient().when(payRemoteGateway.refundCallback(any(RefundCallbackDTO.class)))
            .thenReturn(Result.success(null));
        lenient().when(manager.updateById(any(PayTransactionPO.class))).thenReturn(true);
        lenient().when(payGateway.createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString()))
            .thenReturn("<form action=\"https://openapi-sandbox.dl.alipaydev.com/gateway.do\">mock</form>");
        doAnswer(inv -> {
            PayTransactionPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(PayTransactionPO.class));
        service = new PayServiceImpl(manager, payRefundManager, payGateway,
            payRemoteGateway, transactionManager);
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_persists_pending_record() {
        Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);
        assertNotNull(id);
        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(1, cap.getValue().getStatus());
        assertEquals(0, new BigDecimal("99.99").compareTo(cap.getValue().getAmount()));
        assertEquals("ORDER100", cap.getValue().getActiveOrderNo());
        verify(payGateway, never()).createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_rejects_other_user() {
        assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 999L, 1L, new BigDecimal("99.99"), null));
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_rejects_amount_mismatch() {
        assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("0.01"), null));
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_normalizes_amount_scale() {
        service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.990"), null);

        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(2, cap.getValue().getAmount().scale());
        assertEquals(0, new BigDecimal("99.99").compareTo(cap.getValue().getAmount()));
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_rejects_more_than_two_decimals() {
        BizException ex = assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.991"), null));

        assertEquals(PayCodeEnum.PAY_AMOUNT_MISMATCH.getCode(), ex.getCode());
        verify(manager, never()).save(any(PayTransactionPO.class));
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_fails_when_id_service_unavailable() {
        when(payRemoteGateway.nextId(anyString())).thenReturn(Result.fail("20000", "ID服务不可用"));

        BizException ex = assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null));

        assertEquals(PayCodeEnum.PAY_ID_GENERATE_FAIL.getCode(), ex.getCode());
        verify(manager, never()).save(any(PayTransactionPO.class));
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_rejects_non_pending_order() {
        when(payRemoteGateway.paySnapshot("ORDER500"))
            .thenReturn(Result.success(snapshot("ORDER500", 5L, 1L, "12.00", 2)));
        when(manager.getOne(any())).thenReturn(null);

        assertThrows(BizException.class,
            () -> service.createPayment("ORDER500", 5L, 1L, new BigDecimal("12.00"), null));
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_reuses_pending_transaction() {
        PayTransactionPO existing = new PayTransactionPO();
        existing.setId(99L);
        existing.setOrderNo("ORDER100");
        existing.setStatus(1); // 待支付（PENDING）
        existing.setExpireAt(LocalDateTime.now().plusMinutes(10));
        when(manager.getOne(any())).thenReturn(existing);

        Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

        assertEquals(99L, id.longValue());
        verify(manager, never()).save(any(PayTransactionPO.class));
        verify(payGateway, never()).createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_creates_new_when_pending_expired() {
        PayTransactionPO existing = new PayTransactionPO();
        existing.setId(99L);
        existing.setOrderNo("ORDER100");
        existing.setStatus(1); // 待支付（PENDING）
        existing.setExpireAt(LocalDateTime.now().minusMinutes(1));
        when(manager.getOne(any())).thenReturn(existing);

        Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

        assertNotNull(id);
        assertNotEquals(99L, id.longValue());
        assertEquals(4, existing.getStatus());
        assertNull(existing.getActiveOrderNo());
        verify(manager).updateById(existing);
        verify(manager).save(any(PayTransactionPO.class));
        verify(payGateway, never()).createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_duplicate_active_key_reuses_existing() {
        PayTransactionPO active = new PayTransactionPO();
        active.setId(88L);
        active.setOrderNo("ORDER100");
        active.setPaymentNo("PAY-ACTIVE");
        active.setStatus(1);
        when(manager.getOne(any())).thenReturn(null, active);
        when(manager.save(any(PayTransactionPO.class)))
            .thenThrow(new DuplicateKeyException("duplicate active order"));

        Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

        assertEquals(88L, id.longValue());
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_reuses_success_transaction() {
        PayTransactionPO existing = new PayTransactionPO();
        existing.setId(88L);
        existing.setOrderNo("ORDER100");
        existing.setStatus(2); // 支付成功（SUCCESS）
        when(manager.getOne(any())).thenReturn(existing);

        Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

        assertEquals(88L, id.longValue());
        verify(manager, never()).save(any(PayTransactionPO.class));
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_creates_new_after_failed() {
        PayTransactionPO existing = new PayTransactionPO();
        existing.setId(77L);
        existing.setOrderNo("ORDER100");
        existing.setStatus(3); // 支付失败（FAILED）
        when(manager.getOne(any())).thenReturn(existing);

        Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);

        assertNotNull(id);
        assertNotEquals(77L, id.longValue());
        verify(manager).save(any(PayTransactionPO.class));
        verify(payGateway, never()).createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_unsupported_channel_throws() {
        assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), 2));
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_marks_success() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);
        assertEquals(2, rec.getStatus());
        verify(payRemoteGateway).pay(100L);
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_refunding_payment_does_not_advance_order() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(5);
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);

        assertEquals(5, rec.getStatus());
        verify(payRemoteGateway, never()).pay(anyLong());
        verify(payRemoteGateway, never()).payByOrderNo(anyString());
        verify(manager, never()).updateById(rec);
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_refunded_payment_does_not_advance_order() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(6);
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);

        assertEquals(6, rec.getStatus());
        verify(payRemoteGateway, never()).pay(anyLong());
        verify(payRemoteGateway, never()).payByOrderNo(anyString());
        verify(manager, never()).updateById(rec);
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_frozen_payment_does_not_advance_order() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(7);
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);

        assertEquals(7, rec.getStatus());
        verify(payRemoteGateway, never()).pay(anyLong());
        verify(payRemoteGateway, never()).payByOrderNo(anyString());
        verify(manager, never()).updateById(rec);
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_advances_order_by_order_no() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        rec.setOrderNo("OD20260814001");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);
        assertEquals(2, rec.getStatus());
        verify(payRemoteGateway).payByOrderNo("OD20260814001");
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        PayCallbackDTO dto = new PayCallbackDTO("PAY999", "TRADE999", true, "response");
        assertThrows(BizException.class, () -> service.handleCallback(dto));
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_marks_failed() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", false, "response");
        service.handleCallback(dto);
        assertEquals(3, rec.getStatus());
        verify(payRemoteGateway, never()).pay(any());
    }


    /**
     * 获取相关数据。
     */
    public void getByOrderNo_returns_record_when_exists() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setOrderNo("ORDER100");
        rec.setUserId(1L);
        when(manager.getOne(any())).thenReturn(rec);

        PayTransactionPO result = service.getByOrderNo("ORDER100", 1L);
        assertEquals(1L, result.getId());
        assertEquals("ORDER100", result.getOrderNo());
    }


    /**
     * 获取相关数据。
     */
    public void getByOrderNo_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getByOrderNo("ORDER999", 1L));
    }


    /**
     * 获取相关数据。
     */
    public void getByOrderNo_rejects_other_user() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setOrderNo("ORDER100");
        rec.setUserId(1L);
        when(manager.getOne(any())).thenReturn(rec);

        BizException ex = assertThrows(BizException.class, () -> service.getByOrderNo("ORDER100", 999L));
        assertEquals(PayCodeEnum.PAY_NOT_FOUND.getCode(), ex.getCode());
    }


    /**
     * 获取相关数据。
     */
    public void getPaymentParams_returns_vo_when_exists() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setPaymentNo("PAY123");
        rec.setOrderNo("ORDER100");
        rec.setUserId(1L);
        rec.setAmount(new BigDecimal("99.99"));
        rec.setCurrency("CNY");
        rec.setChannel(1);
        rec.setExpireAt(LocalDateTime.now().plusMinutes(15));
        when(manager.getOne(any())).thenReturn(rec);

        PaymentParamsVO vo = service.getPaymentParams("PAY123", 1L);
        assertEquals("PAY123", vo.getPaymentNo());
        assertEquals("ORDER100", vo.getOrderNo());
        assertEquals(0, new BigDecimal("99.99").compareTo(vo.getAmount()));
        assertEquals("Order ORDER100", vo.getSubject());
        assertEquals("<form action=\"https://openapi-sandbox.dl.alipaydev.com/gateway.do\">mock</form>",
            vo.getPayForm());
    }


    /**
     * 获取相关数据。
     */
    public void getPaymentParams_gateway_failure_throws() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setPaymentNo("PAY123");
        rec.setOrderNo("ORDER100");
        rec.setUserId(1L);
        rec.setAmount(new BigDecimal("99.99"));
        rec.setExpireAt(LocalDateTime.now().plusMinutes(15));
        when(manager.getOne(any())).thenReturn(rec);
        when(payGateway.createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString()))
            .thenThrow(new RuntimeException("alipay down"));

        assertThrows(RuntimeException.class, () -> service.getPaymentParams("PAY123", 1L));
    }


    /**
     * 获取相关数据。
     */
    public void getPaymentParams_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getPaymentParams("PAY999", 1L));
    }


    /**
     * 获取相关数据。
     */
    public void getPaymentParams_rejects_other_user() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setPaymentNo("PAY123");
        rec.setUserId(1L);
        when(manager.getOne(any())).thenReturn(rec);

        BizException ex = assertThrows(BizException.class,
            () -> service.getPaymentParams("PAY123", 999L));
        assertEquals(PayCodeEnum.PAY_NOT_FOUND.getCode(), ex.getCode());
        verify(payGateway, never())
            .createPagePayment(anyString(), any(BigDecimal.class), anyString(), anyString());
    }


    /**
     * 分页查询数据。
     */
    public void page_delegates_to_manager() {
        PayPageDTO dto = new PayPageDTO();
        dto.setStatus(2);
        List<PayTransactionPO> mockRecords = new ArrayList<>();
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(mockRecords);

        CursorPageVO<PayTransactionPO> result = service.page(10L, dto);
        assertNotNull(result);
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 执行 platformPage_delegates_to_manager 方法。
     */
    public void platformPage_delegates_to_manager() {
        PayPageDTO dto = new PayPageDTO();
        List<PayTransactionPO> mockRecords = new ArrayList<>();
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(mockRecords);

        CursorPageVO<PayTransactionPO> result = service.platformPage(dto);
        assertNotNull(result);
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 执行 statistics_delegates_to_mapper 方法。
     */
    public void statistics_delegates_to_mapper() {
        PayPageDTO dto = new PayPageDTO();
        PayStatisticsDTO expected = new PayStatisticsDTO(
            new BigDecimal("1000.00"), new BigDecimal("100.00"), 50L);
        when(manager.statistics(any(), any(), any())).thenReturn(expected);

        PayStatisticsVO result = service.statistics(10L, dto);
        assertEquals(0, new BigDecimal("1000.00").compareTo(result.getTotalAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(result.getRefundAmount()));
        assertEquals(50L, result.getTransactionCount());
        verify(manager).statistics(any(), any(), any());
    }


    /**
     * 执行 statement_delegates_to_mapper 方法。
     */
    public void statement_delegates_to_mapper() {
        PayStatementDTO dto = new PayStatementDTO();
        dto.setStartDate(LocalDate.of(2026, 7, 1));
        dto.setEndDate(LocalDate.of(2026, 7, 31));

        PayStatementResultDTO expected = new PayStatementResultDTO();
        expected.setTotalCount(100L);
        expected.setTotalAmount(new BigDecimal("10000.00"));
        expected.setPaidCount(80L);
        expected.setPaidAmount(new BigDecimal("8000.00"));
        expected.setRefundedCount(5L);
        expected.setRefundedAmount(new BigDecimal("500.00"));
        expected.setFrozenCount(2L);
        expected.setFrozenAmount(new BigDecimal("200.00"));
        when(manager.statement(any(), any())).thenReturn(expected);

        PayStatementVO result = service.statement(dto);

        assertEquals(100L, result.getTotalCount());
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.getTotalAmount()));
        assertEquals(80L, result.getPaidCount());
        assertEquals(0, new BigDecimal("8000.00").compareTo(result.getPaidAmount()));
        assertEquals(5L, result.getRefundedCount());
        assertEquals(0, new BigDecimal("500.00").compareTo(result.getRefundedAmount()));
        assertEquals(2L, result.getFrozenCount());
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getFrozenAmount()));
        verify(manager).statement(any(), any());
    }


    /**
     * 执行 freeze_marks_frozen_when_exists 方法。
     */
    public void freeze_marks_frozen_when_exists() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setPaymentNo("PAY123");
        rec.setStatus(1);
        when(manager.getOne(any())).thenReturn(rec);

        service.freeze("PAY123");
        assertEquals(7, rec.getStatus());
        verify(manager).updateById(rec);
    }


    /**
     * 执行 freeze_missing_throws_biz 方法。
     */
    public void freeze_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.freeze("PAY999"));
    }


    /**
     * 执行 exportTransactions_delegates_to_manager 方法。
     */
    public void exportTransactions_delegates_to_manager() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(new java.util.ArrayList<>());
        assertEquals(0, service.exportTransactions(new PayPageDTO()).size());
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_pay_not_found_throws() {
        when(manager.getById(any())).thenReturn(null);
        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "test");
        assertThrows(BizException.class, () -> service.createRefund(dto));
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_pay_not_success_throws() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(1); // 待支付，不是支付成功（SUCCESS，2）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        when(manager.getById(1L)).thenReturn(payTx);

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "test");
        assertThrows(BizException.class, () -> service.createRefund(dto));
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_amount_exceed_throws() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("50.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("100.00"), "test");
        assertThrows(BizException.class, () -> service.createRefund(dto));
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_rejects_more_than_two_decimals() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("50.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("10.005"), "test");

        BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));
        assertEquals(PayCodeEnum.PAY_AMOUNT_SCALE_INVALID.getCode(), ex.getCode());
        verify(payGateway, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_fails_when_id_service_unavailable() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("50.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(payRemoteGateway.nextId(anyString())).thenReturn(null);

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("10.00"), "test");

        BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));
        assertEquals(PayCodeEnum.PAY_ID_GENERATE_FAIL.getCode(), ex.getCode());
        verify(payGateway, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_success_returns_vo() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundManager.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
        when(payRefundManager.updateStatusIf(
            anyString(), eq(0), eq(1), anyString())).thenReturn(1);
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn("REFUND_TRADE_123");

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "reason");
        com.yirancrazy.minimall.pay.vo.RefundVO vo = service.createRefund(dto);

        assertNotNull(vo);
        assertNotNull(vo.getRefundNo());
        assertEquals("PAY123", vo.getPaymentNo());
        assertEquals(0, new BigDecimal("50.00").compareTo(vo.getAmount()));
        ArgumentCaptor<RefundCallbackDTO> callback =
            ArgumentCaptor.forClass(RefundCallbackDTO.class);
        verify(payRemoteGateway).refundCallback(callback.capture());
        assertEquals(0, new BigDecimal("50.00").compareTo(callback.getValue().getTotalRefundedAmount()));
        assertEquals(false, callback.getValue().getFullyRefunded());
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_atomic_claim_fails_when_not_success() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 读到 SUCCESS，但行级抢占已被并发退款先行置为 REFUNDING
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(0);
        when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("0.00"));

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "test");
        BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));

        assertEquals("40007", ex.getCode());
        verify(payRefundManager).save(any(PayRefundPO.class));
        verify(transactionManager).rollback(any());
        verify(payGateway, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDTO.class));
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_rejects_when_cumulative_exceeds() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("80.00"));

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("30.00"), "test");
        BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));

        assertEquals("40008", ex.getCode());
        verify(payRefundManager, never()).save(any(PayRefundPO.class));
        verify(manager, never()).updateStatusIf("PAY123", 5, 2);
        verify(transactionManager).rollback(any());
        verify(payGateway, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDTO.class));
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_success_records_refund() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundManager.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
        when(payRefundManager.updateStatusIf(
            "REFUND90001", 0, 1, "REFUND_TRADE_123")).thenReturn(1);
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn("REFUND_TRADE_123");

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1");
        // insert 时点快照：同一退款对象后续会被服务置为 SUCCESS，需在插入瞬间记录状态
        final int[] insertStatusSnapshot = new int[1];
        doAnswer(inv -> {
            PayRefundPO p = inv.getArgument(0);
            insertStatusSnapshot[0] = p.getStatus();
            return true;
        }).when(payRefundManager).save(any(PayRefundPO.class));

        RefundVO vo = service.createRefund(dto);

        assertEquals(1, vo.getStatus()); // 退款记录最终 SUCCESS
        ArgumentCaptor<PayRefundPO> cap = ArgumentCaptor.forClass(PayRefundPO.class);
        verify(payRefundManager).save(cap.capture());
        assertEquals("PAY123", cap.getValue().getPaymentNo());
        assertEquals(0, insertStatusSnapshot[0]); // 落库时为 PENDING
        verify(payRefundManager).updateStatusIf(
            "REFUND90001", 0, 1, "REFUND_TRADE_123");
        assertEquals(1, cap.getValue().getStatus()); // 成功后 SUCCESS
        assertEquals("REFUND-REQ-1", cap.getValue().getRefundRequestNo());
        verify(manager).updateStatusIf("PAY123", 5, 2); // 未退满回 SUCCESS
        ArgumentCaptor<RefundCallbackDTO> callback =
            ArgumentCaptor.forClass(RefundCallbackDTO.class);
        verify(payRemoteGateway).refundCallback(callback.capture());
        assertEquals("REFUND-REQ-1", callback.getValue().getRefundRequestNo());
        assertEquals(false, callback.getValue().getFullyRefunded());
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_reuses_existing_request() {
        PayRefundPO existing = new PayRefundPO();
        existing.setRefundNo("REFUND-EXISTING");
        existing.setRefundRequestNo("REFUND-REQ-1");
        existing.setPaymentNo("PAY123");
        existing.setAmount(new BigDecimal("50.00"));
        existing.setStatus(1);
        when(payRefundManager.findByRefundRequestNo("REFUND-REQ-1")).thenReturn(existing);

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1");
        RefundVO result = service.createRefund(dto);

        assertEquals("REFUND-EXISTING", result.getRefundNo());
        verify(manager, never()).getById(anyLong());
        verify(manager, never()).updateStatusIf(anyString(), anyInt(), anyInt());
        verify(payGateway, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDTO.class));
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_recovers_existing_pending_request() {
        PayRefundPO existing = new PayRefundPO();
        existing.setRefundNo("REFUND-STABLE");
        existing.setRefundRequestNo("REFUND-REQ-1");
        existing.setPaymentNo("PAY123");
        existing.setAmount(new BigDecimal("50.00"));
        existing.setReason("reason");
        existing.setStatus(0);
        when(payRefundManager.findByRefundRequestNo("REFUND-REQ-1")).thenReturn(existing);

        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2);
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(payGateway.queryRefund("PAY123", "REFUND-STABLE"))
            .thenReturn(PayGateway.RefundQueryStatus.NOT_FOUND);
        when(payGateway.refund("PAY123", "REFUND-STABLE", new BigDecimal("50.00"), "reason"))
            .thenReturn("REFUND_TRADE_123");
        when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
        when(payRefundManager.updateStatusIf(
            eq("REFUND-STABLE"), eq(0), eq(1), any())).thenReturn(1);

        RefundVO result = service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1"));

        assertEquals("REFUND-STABLE", result.getRefundNo());
        assertEquals(1, result.getStatus());
        verify(payGateway).refund("PAY123", "REFUND-STABLE", new BigDecimal("50.00"), "reason");
        verify(manager).updateStatusIf("PAY123", 5, 2);
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_gateway_exception_keeps_pending_for_retry() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2);
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenThrow(new RuntimeException("gateway timeout"));

        ArgumentCaptor<PayRefundPO> refundCaptor = ArgumentCaptor.forClass(PayRefundPO.class);
        when(payRefundManager.save(refundCaptor.capture())).thenReturn(true);

        assertThrows(BizException.class, () -> service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1")));

        assertEquals(0, refundCaptor.getValue().getStatus());
        verify(payRefundManager, never()).updateStatusIf(
            anyString(), anyInt(), anyInt(), anyString());
        verify(manager, never()).updateStatusIf("PAY123", 5, 2);
        verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDTO.class));
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPendingRefunds_completes_refund_found_in_gateway() {
        PayRefundPO pending = new PayRefundPO();
        pending.setRefundNo("REFUND-STABLE");
        pending.setPaymentNo("PAY123");
        pending.setAmount(new BigDecimal("50.00"));
        pending.setStatus(0);
        pending.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        when(payRefundManager.listPendingRefunds(any(LocalDateTime.class), eq(100)))
            .thenReturn(List.of(pending));
        when(payRefundManager.claimPendingRefund(
            eq("REFUND-STABLE"), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);

        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(payTx);
        when(payGateway.queryRefund("PAY123", "REFUND-STABLE"))
            .thenReturn(PayGateway.RefundQueryStatus.SUCCESS);
        when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
        when(payRefundManager.updateStatusIf(
            eq("REFUND-STABLE"), eq(0), eq(1), any())).thenReturn(1);

        int count = service.scanPendingRefunds();

        assertEquals(1, count);
        verify(payGateway, never()).refund(
            anyString(), anyString(), any(BigDecimal.class), anyString());
        verify(manager).updateStatusIf("PAY123", 5, 2);
        ArgumentCaptor<RefundCallbackDTO> callback =
            ArgumentCaptor.forClass(RefundCallbackDTO.class);
        verify(payRemoteGateway).refundCallback(callback.capture());
        assertEquals(false, callback.getValue().getFullyRefunded());
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_full_refund_marks_refunded() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundManager.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00")).thenReturn(new BigDecimal("100.00"));
        when(payRefundManager.updateStatusIf(
            anyString(), eq(0), eq(1), anyString())).thenReturn(1);
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn("REFUND_TRADE_123");

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("100.00"), "reason");
        service.createRefund(dto);

        verify(manager).updateStatusIf("PAY123", 5, 6); // 累计退满置 REFUNDED
        ArgumentCaptor<RefundCallbackDTO> callback =
            ArgumentCaptor.forClass(RefundCallbackDTO.class);
        verify(payRemoteGateway).refundCallback(callback.capture());
        assertEquals(true, callback.getValue().getFullyRefunded());
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_gateway_success_order_callback_fails_keeps_success() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2);
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundManager.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
        when(payRefundManager.updateStatusIf(
            anyString(), eq(0), eq(1), anyString())).thenReturn(1);
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn("REFUND_TRADE_123");
        when(payRemoteGateway.refundCallback(any(RefundCallbackDTO.class)))
            .thenThrow(new RuntimeException("order unavailable"));

        RefundVO vo = service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                1L, new BigDecimal("50.00"), "reason"));

        assertEquals(1, vo.getStatus());
        verify(manager).updateStatusIf("PAY123", 5, 2);
        verify(payRefundManager, never()).updateById(argThat((PayRefundPO r) -> r.getStatus() == 2));
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanUnnotifiedRefunds_retries_and_marks_notified() {
        PayRefundPO refund = new PayRefundPO();
        refund.setRefundNo("R100");
        refund.setPaymentNo("PAY123");
        refund.setStatus(1);
        refund.setCreateTime(LocalDateTime.now());
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(payRefundManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(refund));
        when(manager.getOne(any())).thenReturn(payTx);
        when(payRefundManager.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
        when(payRemoteGateway.refundCallback(any(RefundCallbackDTO.class)))
            .thenReturn(Result.success(null));
        when(payRefundManager.updateById(refund)).thenReturn(true);

        int count = service.scanUnnotifiedRefunds();

        assertEquals(1, count);
        assertNotNull(refund.getNotifiedAt());
        verify(payRefundManager).updateById(refund);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPaidButOrderPending_calls_pay_when_pending() {
        PayTransactionPO tx = new PayTransactionPO();
        tx.setPaymentNo("PAY1");
        tx.setOrderNo("100");
        tx.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx));
        int count = service.scanPaidButOrderPending();

        assertEquals(1, count);
        verify(payRemoteGateway).pay(100L);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPaidButOrderPending_advances_numeric_order() {
        PayTransactionPO tx = new PayTransactionPO();
        tx.setPaymentNo("PAY1");
        tx.setOrderNo("100");
        tx.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx));
        int count = service.scanPaidButOrderPending();

        assertEquals(1, count);
        verify(payRemoteGateway).pay(100L);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPaidButOrderPending_supports_non_numeric_order_no() {
        PayTransactionPO tx1 = new PayTransactionPO();
        tx1.setPaymentNo("PAY1");
        tx1.setOrderNo("NOT_AN_ID");
        tx1.setStatus(2);
        PayTransactionPO tx2 = new PayTransactionPO();
        tx2.setPaymentNo("PAY2");
        tx2.setOrderNo("200");
        tx2.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx1, tx2));
        int count = service.scanPaidButOrderPending();

        assertEquals(2, count);
        verify(payRemoteGateway).payByOrderNo("NOT_AN_ID");
        verify(payRemoteGateway).pay(200L);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPaidButOrderPending_returns_zero_when_empty() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.Collections.emptyList());

        assertEquals(0, service.scanPaidButOrderPending());
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_null_merchant_defaults_to_zero() {
        Long id = service.createPayment("ORDER300", 3L, null, new BigDecimal("10.00"), null);
        assertNotNull(id);
        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(0L, cap.getValue().getMerchantId());
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_resolves_merchant_from_order() {
        Long id = service.createPayment("ORDER400", 4L, null, new BigDecimal("10.00"), null);

        assertNotNull(id);
        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(42L, cap.getValue().getMerchantId());
    }


    /**
     * 创建业务数据。
     */
    public void createPayment_explicit_alipay_channel_persists() {
        Long id = service.createPayment("ORDER200", 2L, 3L, new BigDecimal("50.00"), 1);
        assertNotNull(id);
        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(1, cap.getValue().getChannel());
        assertEquals(1, cap.getValue().getStatus());
    }

    private static OrderPaySnapshotDTO snapshot(String orderNo, Long userId, Long merchantId,
                                                String amount, int status) {
        return new OrderPaySnapshotDTO(1L, orderNo, userId, merchantId,
            new BigDecimal(amount), status, null);
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_failed_with_null_order_no_skips_feign() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        rec.setOrderNo(null);
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY1", "TRADE1", false, "fail-response");
        service.handleCallback(dto);

        assertEquals(3, rec.getStatus());
        verify(payRemoteGateway, never()).pay(any());
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_amount_mismatch_freezes_without_advancing_order() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        rec.setOrderNo("100");
        rec.setAmount(new BigDecimal("100.00"));
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response", new BigDecimal("99.99"));
        service.handleCallback(dto);

        assertEquals(7, rec.getStatus()); // 已冻结（FROZEN）
        verify(manager).updateById(rec);
        verify(payRemoteGateway, never()).pay(anyLong());
        verify(payRemoteGateway, never()).payByOrderNo(anyString());
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_order_advance_failure_keeps_pending() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        rec.setOrderNo("100");
        rec.setAmount(new BigDecimal("100.00"));
        when(manager.getOne(any())).thenReturn(rec);
        when(payRemoteGateway.pay(100L)).thenThrow(new RuntimeException("order unavailable"));

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response",
            new BigDecimal("100.00"));

        assertThrows(BizException.class, () -> service.handleCallback(dto));
        assertEquals(1, rec.getStatus());
        verify(manager, never()).updateById(rec);
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_already_success_ignores_duplicate() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(2); // 支付成功（SUCCESS）
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);

        assertEquals(2, rec.getStatus());
        verify(manager, never()).updateById(any(PayTransactionPO.class));
        verify(payRemoteGateway, never()).pay(anyLong());
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_failure_does_not_overwrite_success() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(2); // 支付成功（SUCCESS）
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", false, "fail-response");
        service.handleCallback(dto);

        assertEquals(2, rec.getStatus());
        verify(manager, never()).updateById(any(PayTransactionPO.class));
        verify(payRemoteGateway, never()).pay(anyLong());
    }


    /**
     * 处理业务事件。
     */
    public void handleCallback_failure_ignored_when_not_pending() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(3); // 支付失败（FAILED）
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", false, "fail-response");
        service.handleCallback(dto);

        assertEquals(3, rec.getStatus());
        verify(manager, never()).updateById(any(PayTransactionPO.class));
    }


    /**
     * 创建业务数据。
     */
    public void createRefund_non_numeric_order_no_skips_feign() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("OD20260814001");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(manager.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundManager.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
        when(payRefundManager.updateStatusIf(
            anyString(), eq(0), eq(1), anyString())).thenReturn(1);
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn("REFUND_TRADE_123");

        RefundVO vo = service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "reason"));

        assertNotNull(vo);
        verify(payGateway).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        verify(payRemoteGateway, never()).refundCallback(any(RefundCallbackDTO.class));
        verify(manager).updateStatusIf("PAY123", 5, 2); // 未退满回 SUCCESS
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPaidButOrderPending_probe_exception_skips() {
        PayTransactionPO tx1 = new PayTransactionPO();
        tx1.setPaymentNo("PAY1");
        tx1.setOrderNo("100");
        tx1.setStatus(2);
        PayTransactionPO tx2 = new PayTransactionPO();
        tx2.setPaymentNo("PAY2");
        tx2.setOrderNo("200");
        tx2.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx1, tx2));
        when(payRemoteGateway.pay(100L)).thenThrow(new RuntimeException("rpc down"));

        int count = service.scanPaidButOrderPending();

        assertEquals(1, count);
        verify(payRemoteGateway).pay(100L);
        verify(payRemoteGateway).pay(200L);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPaidButOrderPending_advance_failure_skips_count() {
        PayTransactionPO tx = new PayTransactionPO();
        tx.setPaymentNo("PAY1");
        tx.setOrderNo("100");
        tx.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx));
        when(payRemoteGateway.pay(100L)).thenReturn(Result.fail("40012", "advance failed"));

        int count = service.scanPaidButOrderPending();

        assertEquals(0, count);
        verify(payRemoteGateway).pay(100L);
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPaidButOrderPending_batches_with_window_and_cursor() {
        List<PayTransactionPO> firstBatch = new ArrayList<>(100);
        for (long i = 1; i <= 100; i++) {
            PayTransactionPO tx = new PayTransactionPO();
            tx.setId(i);
            tx.setPaymentNo("PAY" + i);
            tx.setOrderNo(String.valueOf(i));
            tx.setStatus(2);
            firstBatch.add(tx);
        }
        PayTransactionPO last = new PayTransactionPO();
        last.setId(101L);
        last.setPaymentNo("PAY101");
        last.setOrderNo("101");
        last.setStatus(2);
        when(manager.list(any(Wrapper.class))).thenReturn(firstBatch).thenReturn(List.of(last));
        service.scanPaidButOrderPending();

        ArgumentCaptor<Wrapper<PayTransactionPO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(manager, times(2)).list(captor.capture());
        LambdaQueryWrapper<PayTransactionPO> first =
            (LambdaQueryWrapper<PayTransactionPO>) captor.getAllValues().get(0);
        LambdaQueryWrapper<PayTransactionPO> second =
            (LambdaQueryWrapper<PayTransactionPO>) captor.getAllValues().get(1);
        String firstSql = first.getSqlSegment();
        String secondSql = second.getSqlSegment();
        // 每批均带 LIMIT 100 分页
        assertTrue(firstSql.contains("LIMIT 100"));
        assertTrue(secondSql.contains("LIMIT 100"));
        // 时间窗：paidAt >= 窗口起点 AND paidAt < 阈值，对应参数各一个 LocalDateTime
        assertTrue(firstSql.contains("paid_at >="));
        assertTrue(firstSql.contains("paid_at <"));
        assertEquals(2, first.getParamNameValuePairs().values().stream()
            .filter(LocalDateTime.class::isInstance).count());
        // 第二批游标推进为 id > 100，且按 id 升序取批
        assertTrue(secondSql.contains("id >"));
        assertTrue(secondSql.contains("ORDER BY id ASC"));
        assertTrue(second.getParamNameValuePairs().containsValue(100L));
    }


    /**
     * 扫描并处理待办数据。
     */
    public void scanPaidButOrderPending_stops_after_max_batches() {
        List<PayTransactionPO> fullBatch = new ArrayList<>(100);
        for (long i = 1; i <= 100; i++) {
            PayTransactionPO tx = new PayTransactionPO();
            tx.setId(i);
            tx.setPaymentNo("PAY" + i);
            tx.setOrderNo(String.valueOf(i));
            tx.setStatus(2);
            fullBatch.add(tx);
        }
        when(manager.list(any(Wrapper.class))).thenReturn(fullBatch);
        service.scanPaidButOrderPending();

        verify(manager, times(10)).list(any(Wrapper.class));
    }


    /**
     * 分页查询数据。
     */
    public void page_with_full_filters_delegates_to_manager() {
        PayPageDTO dto = new PayPageDTO();
        dto.setCursor("MTAw");
        dto.setStatus(2);
        dto.setStartTime(java.time.LocalDateTime.of(2026, 7, 1, 0, 0));
        dto.setEndTime(java.time.LocalDateTime.of(2026, 7, 31, 23, 59));
        dto.setLimit(20);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(new java.util.ArrayList<>());

        CursorPageVO<PayTransactionPO> result = service.page(10L, dto);

        assertNotNull(result);
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 执行 exportTransactions_empty_filters_returns_empty 方法。
     */
    public void exportTransactions_empty_filters_returns_empty() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(new java.util.ArrayList<>());

        java.util.List<PayTransactionPO> result = service.exportTransactions(new PayPageDTO());

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 执行 channel_response_is_encrypted_at_rest 方法。
     */
    public void channel_response_is_encrypted_at_rest() {
        String plain = "{\"trade_no\":\"T20260813\",\"amount\":\"9.90\",\"sign\":\"abc\"}";
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes();

        String cipher = com.yirancrazy.minimall.common.security.AesEncryptor.encrypt(plain, key);

        assertNotNull(cipher);
        org.junit.jupiter.api.Assertions.assertNotEquals(plain, cipher);
        assertEquals(plain, com.yirancrazy.minimall.common.security.AesEncryptor.decrypt(cipher, key));
    }


    /**
     * 处理业务事件。
     */
    public void handleAlipayCallback_verify_failure_returns_false() {
        when(payGateway.verifyCallback(any())).thenThrow(new RuntimeException("bad signature"));

        boolean ack = service.handleAlipayCallback(new AlipayCallbackDTO(java.util.Map.of(
            "out_trade_no", "PAY1", "trade_status", "TRADE_SUCCESS")));

        org.junit.jupiter.api.Assertions.assertFalse(ack);
        verify(payGateway).verifyCallback(any());
    }


    /**
     * 处理业务事件。
     */
    public void handleAlipayCallback_success_advances_order() {
        PayTransactionPO po = new PayTransactionPO();
        po.setId(1L);
        po.setPaymentNo("PAY1");
        po.setOrderNo("ORDER100");
        po.setAmount(new BigDecimal("10.00"));
        po.setStatus(1);
        when(manager.getOne(any(Wrapper.class))).thenReturn(po);
        when(payGateway.verifyCallback(any())).thenReturn("T1");

        boolean ack = service.handleAlipayCallback(new AlipayCallbackDTO(java.util.Map.of(
            "out_trade_no", "PAY1", "trade_status", "TRADE_SUCCESS", "total_amount", "10.00")));

        assertTrue(ack);
        verify(payRemoteGateway).payByOrderNo("ORDER100");
    }
}
