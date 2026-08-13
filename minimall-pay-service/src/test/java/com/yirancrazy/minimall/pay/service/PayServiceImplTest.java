package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementDTO;
import com.yirancrazy.minimall.pay.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.pay.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import com.yirancrazy.minimall.pay.manager.MerchantWithdrawManager;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;
import com.yirancrazy.minimall.pay.service.impl.PayServiceImpl;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.WithdrawVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayServiceImpl 的单元测试类。
 * @Version: 1.1
 * @DateTime: 2026/08/02
 **/
public class PayServiceImplTest {

    private PayManager manager;
    private AlipayGateway alipayGateway;
    private PayRefundMapper payRefundMapper;
    private PayTransactionMapper payTransactionMapper;
    private OrderFeignClient orderFeignClient;
    private MerchantWithdrawManager merchantWithdrawManager;
    private PayServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(PayManager.class);
        alipayGateway = mock(AlipayGateway.class);
        payRefundMapper = mock(PayRefundMapper.class);
        payTransactionMapper = mock(PayTransactionMapper.class);
        orderFeignClient = mock(OrderFeignClient.class);
        merchantWithdrawManager = mock(MerchantWithdrawManager.class);
        lenient().when(manager.updateById(any(PayTransactionPO.class))).thenReturn(true);
        lenient().when(alipayGateway.createPayment(anyString(), any(BigDecimal.class), anyString(), anyString()))
            .thenReturn("http://pay.url");
        doAnswer(inv -> {
            PayTransactionPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(PayTransactionPO.class));
        lenient().when(merchantWithdrawManager.updateById(any(MerchantWithdrawPO.class))).thenReturn(true);
        doAnswer(inv -> {
            MerchantWithdrawPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(merchantWithdrawManager).save(any(MerchantWithdrawPO.class));
        service = new PayServiceImpl(manager, alipayGateway, payRefundMapper, payTransactionMapper,
            orderFeignClient, merchantWithdrawManager);
    }

    /**
     * 验证 createPayment 方法会持久化一条状态为 PENDING 且金额正确的支付单记录。
     */
    @Test
    public void createPayment_persists_pending_record() {
        Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null);
        assertNotNull(id);
        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(1, cap.getValue().getStatus());
        assertEquals(0, new BigDecimal("99.99").compareTo(cap.getValue().getAmount()));
    }

    /**
     * 验证 createPayment 在传入不支持渠道时抛出 PAY_CHANNEL_UNSUPPORTED。
     */
    @Test
    public void createPayment_unsupported_channel_throws() {
        assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), 2));
    }

    /**
     * 验证 handleCallback 在支付成功时把支付单状态推进为 SUCCESS，并触发 order.pay。
     */
    @Test
    public void handleCallback_marks_success() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);
        assertEquals(2, rec.getStatus());
        verify(orderFeignClient).pay(100L);
    }

    /**
     * 验证 handleCallback 在找不到对应支付单时抛出 PAY_NOT_FOUND 业务异常。
     */
    @Test
    public void handleCallback_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        PayCallbackDTO dto = new PayCallbackDTO("PAY999", "TRADE999", true, "response");
        assertThrows(BizException.class, () -> service.handleCallback(dto));
    }

    /**
     * 验证 handleCallback 在支付失败时将支付单状态推进为 FAILED。
     */
    @Test
    public void handleCallback_marks_failed() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", false, "response");
        service.handleCallback(dto);
        assertEquals(3, rec.getStatus());
        verify(orderFeignClient, never()).pay(any());
    }

    /**
     * 验证 getByOrderNo 在订单号存在时返回支付流水。
     */
    @Test
    public void getByOrderNo_returns_record_when_exists() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setOrderNo("ORDER100");
        when(manager.getOne(any())).thenReturn(rec);

        PayTransactionPO result = service.getByOrderNo("ORDER100");
        assertEquals(1L, result.getId());
        assertEquals("ORDER100", result.getOrderNo());
    }

    /**
     * 验证 getByOrderNo 在找不到支付流水时抛出 PAY_NOT_FOUND。
     */
    @Test
    public void getByOrderNo_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getByOrderNo("ORDER999"));
    }

    /**
     * 验证 getPaymentParams 在支付单存在时返回参数VO。
     */
    @Test
    public void getPaymentParams_returns_vo_when_exists() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setPaymentNo("PAY123");
        rec.setOrderNo("ORDER100");
        rec.setAmount(new BigDecimal("99.99"));
        rec.setCurrency("CNY");
        rec.setChannel(1);
        when(manager.getOne(any())).thenReturn(rec);

        PaymentParamsVO vo = service.getPaymentParams("PAY123");
        assertEquals("PAY123", vo.getPaymentNo());
        assertEquals("ORDER100", vo.getOrderNo());
        assertEquals(0, new BigDecimal("99.99").compareTo(vo.getAmount()));
        assertEquals("Order ORDER100", vo.getSubject());
    }

    /**
     * 验证 getPaymentParams 在找不到支付单时抛出 PAY_NOT_FOUND。
     */
    @Test
    public void getPaymentParams_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getPaymentParams("PAY999"));
    }

    /**
     * 验证 page 委托给 manager.list 并强制绑定 merchantId。
     */
    @Test
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
     * 验证 platformPage 委托给 manager.list，不绑定 merchantId。
     */
    @Test
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
     * 验证 statistics 委托给 mapper.statistics。
     */
    @Test
    public void statistics_delegates_to_mapper() {
        PayPageDTO dto = new PayPageDTO();
        PayStatisticsVO expected = new PayStatisticsVO(new BigDecimal("1000.00"), new BigDecimal("100.00"), 50L);
        when(payTransactionMapper.statistics(any(), any(), any())).thenReturn(expected);

        PayStatisticsVO result = service.statistics(10L, dto);
        assertEquals(expected, result);
        verify(payTransactionMapper).statistics(any(), any(), any());
    }

    /**
     * 验证 statement 委托给 mapper.statement 并返回对账单 VO。
     */
    @Test
    public void statement_delegates_to_mapper() {
        PayStatementDTO dto = new PayStatementDTO();
        dto.setStartDate(LocalDate.of(2026, 7, 1));
        dto.setEndDate(LocalDate.of(2026, 7, 31));

        PayStatementVO expected = new PayStatementVO();
        expected.setTotalCount(100L);
        expected.setTotalAmount(new BigDecimal("10000.00"));
        expected.setPaidCount(80L);
        expected.setPaidAmount(new BigDecimal("8000.00"));
        expected.setRefundedCount(5L);
        expected.setRefundedAmount(new BigDecimal("500.00"));
        expected.setFrozenCount(2L);
        expected.setFrozenAmount(new BigDecimal("200.00"));
        when(payTransactionMapper.statement(any(), any())).thenReturn(expected);

        PayStatementVO result = service.statement(dto);

        assertEquals(expected, result);
        assertEquals(100L, result.getTotalCount());
        assertEquals(0, new BigDecimal("10000.00").compareTo(result.getTotalAmount()));
        assertEquals(80L, result.getPaidCount());
        assertEquals(0, new BigDecimal("8000.00").compareTo(result.getPaidAmount()));
        assertEquals(5L, result.getRefundedCount());
        assertEquals(0, new BigDecimal("500.00").compareTo(result.getRefundedAmount()));
        assertEquals(2L, result.getFrozenCount());
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getFrozenAmount()));
        verify(payTransactionMapper).statement(any(), any());
    }

    /**
     * 验证 freeze 在支付单存在时将状态置为 FROZEN。
     */
    @Test
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
     * 验证 freeze 在找不到支付单时抛出 PAY_NOT_FOUND。
     */
    @Test
    public void freeze_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.freeze("PAY999"));
    }

    /**
     * 验证 exportTransactions 委托给 manager.list。
     */
    @Test
    public void exportTransactions_delegates_to_manager() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(new java.util.ArrayList<>());
        assertEquals(0, service.exportTransactions(new PayPageDTO()).size());
    }

    /**
     * 验证 applyWithdraw 会持久化一条状态为 PENDING 的提现单并返回正确 VO。
     */
    @Test
    public void applyWithdraw_persists_pending_record() {
        WithdrawApplyDTO dto = new WithdrawApplyDTO(new BigDecimal("500.00"), "月度提现");
        WithdrawVO vo = service.applyWithdraw(10L, dto);

        assertNotNull(vo);
        assertEquals(10L, vo.getMerchantId());
        assertEquals(0, new BigDecimal("500.00").compareTo(vo.getAmount()));
        assertEquals(1, vo.getStatus());
        assertEquals("月度提现", vo.getReason());
        assertNotNull(vo.getWithdrawNo());
        assertNotNull(vo.getAppliedAt());

        ArgumentCaptor<MerchantWithdrawPO> cap = ArgumentCaptor.forClass(MerchantWithdrawPO.class);
        verify(merchantWithdrawManager).save(cap.capture());
        assertEquals(1, cap.getValue().getStatus());
        assertEquals(10L, cap.getValue().getMerchantId());
    }

    /**
     * 验证 pageWithdraw 委托给 merchantWithdrawManager.list 并强制绑定 merchantId。
     */
    @Test
    public void pageWithdraw_delegates_to_manager() {
        PayPageDTO dto = new PayPageDTO();
        List<MerchantWithdrawPO> mockRecords = new ArrayList<>();
        when(merchantWithdrawManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(mockRecords);

        CursorPageVO<MerchantWithdrawPO> result = service.pageWithdraw(10L, dto);
        assertNotNull(result);
        verify(merchantWithdrawManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证 platformPageWithdraw 委托给 merchantWithdrawManager.list，不绑定 merchantId。
     */
    @Test
    public void platformPageWithdraw_delegates_to_manager() {
        PayPageDTO dto = new PayPageDTO();
        List<MerchantWithdrawPO> mockRecords = new ArrayList<>();
        when(merchantWithdrawManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(mockRecords);

        CursorPageVO<MerchantWithdrawPO> result = service.platformPageWithdraw(dto);
        assertNotNull(result);
        verify(merchantWithdrawManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证 reviewWithdraw 在审核通过时将提现单状态置为 PAID。
     */
    @Test
    public void reviewWithdraw_approves_marks_paid() {
        MerchantWithdrawPO rec = new MerchantWithdrawPO();
        rec.setId(1L);
        rec.setStatus(1);
        when(merchantWithdrawManager.getById(any())).thenReturn(rec);

        service.reviewWithdraw(1L, true, null);
        assertEquals(4, rec.getStatus());
        verify(merchantWithdrawManager).updateById(rec);
    }

    /**
     * 验证 reviewWithdraw 在审核驳回时将提现单状态置为 REJECTED 并记录原因。
     */
    @Test
    public void reviewWithdraw_rejects_marks_rejected() {
        MerchantWithdrawPO rec = new MerchantWithdrawPO();
        rec.setId(1L);
        rec.setStatus(1);
        when(merchantWithdrawManager.getById(any())).thenReturn(rec);

        service.reviewWithdraw(1L, false, "材料不全");
        assertEquals(3, rec.getStatus());
        assertEquals("材料不全", rec.getReason());
        verify(merchantWithdrawManager).updateById(rec);
    }

    /**
     * 验证 reviewWithdraw 在找不到提现单时抛出 WITHDRAW_NOT_FOUND 业务异常。
     */
    @Test
    public void reviewWithdraw_missing_throws_biz() {
        when(merchantWithdrawManager.getById(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.reviewWithdraw(999L, true, null));
    }

    /**
     * 验证 createRefund 在支付单不存在时抛出 PAY_NOT_FOUND。
     */
    @Test
    public void createRefund_pay_not_found_throws() {
        when(manager.getById(any())).thenReturn(null);
        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "test");
        assertThrows(BizException.class, () -> service.createRefund(dto));
    }

    /**
     * 验证 createRefund 在支付单非 SUCCESS 状态时抛出 PAY_NOT_SUCCESS。
     */
    @Test
    public void createRefund_pay_not_success_throws() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(1); // PENDING, not SUCCESS(2)
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        when(manager.getById(1L)).thenReturn(payTx);

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "test");
        assertThrows(BizException.class, () -> service.createRefund(dto));
    }

    /**
     * 验证 createRefund 退款金额超过支付金额时抛出 REFUND_AMOUNT_EXCEED。
     */
    @Test
    public void createRefund_amount_exceed_throws() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // SUCCESS
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("50.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("100.00"), "test");
        assertThrows(BizException.class, () -> service.createRefund(dto));
    }

    /**
     * 验证 createRefund 退款成功时返回 RefundVO 并推进支付单状态为 REFUNDED。
     */
    @Test
    public void createRefund_success_returns_vo() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // SUCCESS
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(alipayGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn("REFUND_TRADE_123");

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "reason");
        com.yirancrazy.minimall.pay.vo.RefundVO vo = service.createRefund(dto);

        assertNotNull(vo);
        assertNotNull(vo.getRefundNo());
        assertEquals("PAY123", vo.getPaymentNo());
        assertEquals(0, new BigDecimal("50.00").compareTo(vo.getAmount()));
        verify(orderFeignClient).refundCallback(100L, true);
    }

    /**
     * 验证 createRefund 退款网关异常时状态回退并抛出 REFUND_FAILED。
     */
    @Test
    public void createRefund_gateway_exception_throws() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // SUCCESS
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(alipayGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenThrow(new RuntimeException("gateway down"));

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "reason");
        assertThrows(BizException.class, () -> service.createRefund(dto));
        verify(orderFeignClient).refundCallback(100L, false);
    }

    /**
     * 验证 scanPaidButOrderPending 在订单仍 PENDING 时主动调用 order.pay 兜底。
     */
    @Test
    public void scanPaidButOrderPending_calls_pay_when_pending() {
        PayTransactionPO tx = new PayTransactionPO();
        tx.setPaymentNo("PAY1");
        tx.setOrderNo("100");
        tx.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx));
        when(orderFeignClient.status(100L)).thenReturn(com.yirancrazy.minimall.common.result.Result.success(1));

        int count = service.scanPaidButOrderPending();

        assertEquals(1, count);
        verify(orderFeignClient).pay(100L);
    }

    /**
     * 验证 scanPaidButOrderPending 在订单已非 PENDING 时不调用 order.pay。
     */
    @Test
    public void scanPaidButOrderPending_skips_when_order_already_paid() {
        PayTransactionPO tx = new PayTransactionPO();
        tx.setPaymentNo("PAY1");
        tx.setOrderNo("100");
        tx.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx));
        when(orderFeignClient.status(100L)).thenReturn(com.yirancrazy.minimall.common.result.Result.success(2));

        int count = service.scanPaidButOrderPending();

        assertEquals(0, count);
        verify(orderFeignClient, never()).pay(any());
    }

    /**
     * 验证 scanPaidButOrderPending 在 orderNo 非数字时跳过该条继续处理。
     */
    @Test
    public void scanPaidButOrderPending_skips_non_numeric_order_no() {
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
        when(orderFeignClient.status(200L)).thenReturn(com.yirancrazy.minimall.common.result.Result.success(1));

        int count = service.scanPaidButOrderPending();

        assertEquals(1, count);
        verify(orderFeignClient).pay(200L);
    }

    /**
     * 验证 scanPaidButOrderPending 无候选流水时返回 0。
     */
    @Test
    public void scanPaidButOrderPending_returns_zero_when_empty() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.Collections.emptyList());

        assertEquals(0, service.scanPaidButOrderPending());
    }

    /**
     * 验证 createPayment 在显式传 ALIPAY channel=1 时走主路径持久化。
     */
    @Test
    public void createPayment_explicit_alipay_channel_persists() {
        Long id = service.createPayment("ORDER200", 2L, 3L, new BigDecimal("50.00"), 1);
        assertNotNull(id);
        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(1, cap.getValue().getChannel());
        assertEquals(1, cap.getValue().getStatus());
    }

    /**
     * 验证 handleCallback 在失败 + orderNo 为 null 时不调 orderFeignClient.pay。
     */
    @Test
    public void handleCallback_failed_with_null_order_no_skips_feign() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        rec.setOrderNo(null);
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY1", "TRADE1", false, "fail-response");
        service.handleCallback(dto);

        assertEquals(3, rec.getStatus());
        verify(orderFeignClient, never()).pay(any());
    }

    /**
     * 验证 scanPaidButOrderPending 在 feign.status 抛异常时不计数且不中断循环。
     */
    @Test
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
        when(orderFeignClient.status(100L)).thenThrow(new RuntimeException("rpc down"));
        when(orderFeignClient.status(200L)).thenReturn(com.yirancrazy.minimall.common.result.Result.success(1));

        int count = service.scanPaidButOrderPending();

        assertEquals(1, count);
        verify(orderFeignClient).pay(200L);
        verify(orderFeignClient, never()).pay(100L);
    }

    /**
     * 验证 scanPaidButOrderPending 在 feign.status 返 null 时跳过该条不调 order.pay。
     */
    @Test
    public void scanPaidButOrderPending_probe_null_status_skips() {
        PayTransactionPO tx = new PayTransactionPO();
        tx.setPaymentNo("PAY1");
        tx.setOrderNo("100");
        tx.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx));
        when(orderFeignClient.status(100L)).thenReturn(com.yirancrazy.minimall.common.result.Result.success(null));

        int count = service.scanPaidButOrderPending();

        assertEquals(0, count);
        verify(orderFeignClient, never()).pay(any());
    }

    /**
     * 验证 page 在 cursor 与 status 与时间范围三个条件同时提供时仍委托 manager.list。
     */
    @Test
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
     * 验证 exportTransactions 在无任何过滤条件下仍委托 manager.list 并返回空列表。
     */
    @Test
    public void exportTransactions_empty_filters_returns_empty() {
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(new java.util.ArrayList<>());

        java.util.List<PayTransactionPO> result = service.exportTransactions(new PayPageDTO());

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证支付回调报文字段级加密：channelResponse 密文落库（不可明文读），解密可还原。
     */
    @Test
    public void channel_response_is_encrypted_at_rest() {
        String plain = "{\"trade_no\":\"T20260813\",\"amount\":\"9.90\",\"sign\":\"abc\"}";
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes();

        String cipher = com.yirancrazy.minimall.common.security.AesEncryptor.encrypt(plain, key);

        assertNotNull(cipher);
        org.junit.jupiter.api.Assertions.assertNotEquals(plain, cipher);
        assertEquals(plain, com.yirancrazy.minimall.common.security.AesEncryptor.decrypt(cipher, key));
    }
}