package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;
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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.pay.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import com.yirancrazy.minimall.pay.manager.MerchantWithdrawManager;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;
import com.yirancrazy.minimall.pay.service.impl.PayServiceImpl;
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
     * 验证 page 委托给 manager.page 并强制绑定 merchantId。
     */
    @Test
    public void page_delegates_to_manager() {
        PayPageDTO dto = new PayPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        dto.setStatus(2);
        IPage<PayTransactionPO> expected = new Page<>(1, 10);
        when(manager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<PayTransactionPO> result = service.page(10L, dto);
        assertEquals(expected, result);
        verify(manager).page(any(IPage.class), any());
    }

    /**
     * 验证 platformPage 委托给 manager.page，不绑定 merchantId。
     */
    @Test
    public void platformPage_delegates_to_manager() {
        PayPageDTO dto = new PayPageDTO();
        IPage<PayTransactionPO> expected = new Page<>(1, 20);
        when(manager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<PayTransactionPO> result = service.platformPage(dto);
        assertEquals(expected, result);
        verify(manager).page(any(IPage.class), any());
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
     * 验证 pageWithdraw 委托给 merchantWithdrawManager.page 并强制绑定 merchantId。
     */
    @Test
    public void pageWithdraw_delegates_to_manager() {
        PayPageDTO dto = new PayPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        IPage<MerchantWithdrawPO> expected = new Page<>(1, 10);
        when(merchantWithdrawManager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<MerchantWithdrawPO> result = service.pageWithdraw(10L, dto);
        assertEquals(expected, result);
        verify(merchantWithdrawManager).page(any(IPage.class), any());
    }

    /**
     * 验证 platformPageWithdraw 委托给 merchantWithdrawManager.page，不绑定 merchantId。
     */
    @Test
    public void platformPageWithdraw_delegates_to_manager() {
        PayPageDTO dto = new PayPageDTO();
        IPage<MerchantWithdrawPO> expected = new Page<>(1, 20);
        when(merchantWithdrawManager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<MerchantWithdrawPO> result = service.platformPageWithdraw(dto);
        assertEquals(expected, result);
        verify(merchantWithdrawManager).page(any(IPage.class), any());
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
}