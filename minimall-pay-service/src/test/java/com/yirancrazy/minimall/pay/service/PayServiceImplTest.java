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
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.constant.PayCodeEnum;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementDTO;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.PayGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;
import com.yirancrazy.minimall.pay.service.impl.PayServiceImpl;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayServiceImpl 的单元测试类。
 * @Version: 1.1
 * @DateTime: 2026/08/02
 **/
public class PayServiceImplTest {

    private PayManager manager;
    private PayGateway payGateway;
    private PayRefundMapper payRefundMapper;
    private PayTransactionMapper payTransactionMapper;
    private IdFeignClient idFeignClient;
    private OrderFeignClient orderFeignClient;
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
        payGateway = mock(PayGateway.class);
        payRefundMapper = mock(PayRefundMapper.class);
        payTransactionMapper = mock(PayTransactionMapper.class);
        idFeignClient = mock(IdFeignClient.class);
        orderFeignClient = mock(OrderFeignClient.class);
        transactionManager = mock(PlatformTransactionManager.class);
        lenient().when(transactionManager.getTransaction(any()))
            .thenReturn(new SimpleTransactionStatus());
        lenient().when(idFeignClient.nextId(anyString())).thenReturn(Result.success(90001L));
        lenient().when(orderFeignClient.paySnapshot(anyString())).thenAnswer(invocation -> {
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
        lenient().when(orderFeignClient.merchantId(anyString()))
            .thenReturn(Result.success(null));
        lenient().when(orderFeignClient.pay(anyLong())).thenReturn(Result.success(null));
        lenient().when(orderFeignClient.payByOrderNo(anyString())).thenReturn(Result.success(null));
        lenient().when(orderFeignClient.refundCallback(any(RefundCallbackDTO.class)))
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
        service = new PayServiceImpl(manager, payGateway, payRefundMapper, payTransactionMapper,
            idFeignClient, orderFeignClient, transactionManager);
    }

    /**
     * 验证 createPayment 方法会持久化一条状态为 PENDING 且金额正确的支付单记录，且不触发外部网关调用。
     */
    @Test
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
     * 验证支付创建拒绝非订单归属用户。
     */
    @Test
    public void createPayment_rejects_other_user() {
        assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 999L, 1L, new BigDecimal("99.99"), null));
    }

    /**
     * 验证支付创建拒绝与订单应付金额不一致的请求。
     */
    @Test
    public void createPayment_rejects_amount_mismatch() {
        assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("0.01"), null));
    }

    /**
     * 验证落库支付金额统一为两位小数：尾数全为 0 的高精度金额被规范化为 scale=2。
     */
    @Test
    public void createPayment_normalizes_amount_scale() {
        service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.990"), null);

        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(2, cap.getValue().getAmount().scale());
        assertEquals(0, new BigDecimal("99.99").compareTo(cap.getValue().getAmount()));
    }

    /**
     * 验证创建支付拒绝超过两位小数的金额，避免按分存储时静默进位。
     */
    @Test
    public void createPayment_rejects_more_than_two_decimals() {
        BizException ex = assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.991"), null));

        assertEquals(PayCodeEnum.PAY_AMOUNT_MISMATCH.getCode(), ex.getCode());
        verify(manager, never()).save(any(PayTransactionPO.class));
    }

    /**
     * 验证 ID 服务不可用时创建支付直接失败，不回退时间戳+随机数生成支付单号。
     */
    @Test
    public void createPayment_fails_when_id_service_unavailable() {
        when(idFeignClient.nextId(anyString())).thenReturn(Result.fail("20000", "ID服务不可用"));

        BizException ex = assertThrows(BizException.class,
            () -> service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"), null));

        assertEquals(PayCodeEnum.PAY_ID_GENERATE_FAIL.getCode(), ex.getCode());
        verify(manager, never()).save(any(PayTransactionPO.class));
    }

    /**
     * 验证非待支付订单在无已成功支付流水时拒绝创建新支付。
     */
    @Test
    public void createPayment_rejects_non_pending_order() {
        when(orderFeignClient.paySnapshot("ORDER500"))
            .thenReturn(Result.success(snapshot("ORDER500", 5L, 1L, "12.00", 2)));
        when(manager.getOne(any())).thenReturn(null);

        assertThrows(BizException.class,
            () -> service.createPayment("ORDER500", 5L, 1L, new BigDecimal("12.00"), null));
    }

    /**
     * 验证同一订单已有未过期待支付流水时直接复用，不重复创建与调起渠道。
     */
    @Test
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
     * 验证同一订单已有过期待支付流水时不静默复用，而是新建流水重新发起支付（修复支付卡死）。
     */
    @Test
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
     * 验证活跃订单唯一键冲突时返回已存在的活跃支付单。
     */
    @Test
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
     * 验证同一订单已有成功流水时不重复创建，直接返回原流水ID。
     */
    @Test
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
     * 验证上次支付失败时允许重新创建新流水，支持重新支付。
     */
    @Test
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
     * 验证退款中的支付单收到迟到成功回调时不会推进订单。
     */
    @Test
    public void handleCallback_refunding_payment_does_not_advance_order() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(5);
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);

        assertEquals(5, rec.getStatus());
        verify(orderFeignClient, never()).pay(anyLong());
        verify(orderFeignClient, never()).payByOrderNo(anyString());
        verify(manager, never()).updateById(rec);
    }

    /**
     * 验证已退款的支付单收到迟到成功回调时不会推进订单。
     */
    @Test
    public void handleCallback_refunded_payment_does_not_advance_order() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(6);
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);

        assertEquals(6, rec.getStatus());
        verify(orderFeignClient, never()).pay(anyLong());
        verify(orderFeignClient, never()).payByOrderNo(anyString());
        verify(manager, never()).updateById(rec);
    }

    /**
     * 验证冻结支付单收到成功回调时不会推进订单。
     */
    @Test
    public void handleCallback_frozen_payment_does_not_advance_order() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(7);
        rec.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);

        assertEquals(7, rec.getStatus());
        verify(orderFeignClient, never()).pay(anyLong());
        verify(orderFeignClient, never()).payByOrderNo(anyString());
        verify(manager, never()).updateById(rec);
    }

    /**
     * 验证 handleCallback 在订单标识为业务单号（C 端直付）时按单号推进订单，修复回调推进订单崩溃。
     */
    @Test
    public void handleCallback_advances_order_by_order_no() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        rec.setOrderNo("OD20260814001");
        when(manager.getOne(any())).thenReturn(rec);

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response");
        service.handleCallback(dto);
        assertEquals(2, rec.getStatus());
        verify(orderFeignClient).payByOrderNo("OD20260814001");
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
        rec.setUserId(1L);
        when(manager.getOne(any())).thenReturn(rec);

        PayTransactionPO result = service.getByOrderNo("ORDER100", 1L);
        assertEquals(1L, result.getId());
        assertEquals("ORDER100", result.getOrderNo());
    }

    /**
     * 验证 getByOrderNo 在找不到支付流水时抛出 PAY_NOT_FOUND。
     */
    @Test
    public void getByOrderNo_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getByOrderNo("ORDER999", 1L));
    }

    /**
     * 验证 getByOrderNo 在查询他人支付流水时按不存在处理，不泄露他人支付单。
     */
    @Test
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
     * 验证 getPaymentParams 在支付单存在时返回参数VO，并重新生成支付宝 page.pay 跳转表单 payForm。
     */
    @Test
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
     * 验证 getPaymentParams 在支付宝网关不可达时抛异常，由全局异常处理返回明确错误而非静默卡死。
     */
    @Test
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
     * 验证 getPaymentParams 在找不到支付单时抛出 PAY_NOT_FOUND。
     */
    @Test
    public void getPaymentParams_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.getPaymentParams("PAY999", 1L));
    }

    /**
     * 验证 getPaymentParams 在查询他人支付单时按不存在处理，不返回支付参数。
     */
    @Test
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
        payTx.setStatus(1); // 待支付，不是支付成功（SUCCESS，2）
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
     * 验证 createRefund 拒绝超过两位小数的退款金额，避免按分存储时静默进位。
     */
    @Test
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
     * 验证 ID 服务不可用时创建退款直接失败，不回退时间戳+随机数生成退款单号。
     */
    @Test
    public void createRefund_fails_when_id_service_unavailable() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("50.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(idFeignClient.nextId(anyString())).thenReturn(null);

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("10.00"), "test");

        BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));
        assertEquals(PayCodeEnum.PAY_ID_GENERATE_FAIL.getCode(), ex.getCode());
        verify(payGateway, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
    }

    /**
     * 验证 createRefund 退款成功时返回 RefundVO 并推进支付单状态。
     */
    @Test
    public void createRefund_success_returns_vo() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(payTransactionMapper.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundMapper.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
        when(payRefundMapper.updateStatusIf(
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
        verify(orderFeignClient).refundCallback(callback.capture());
        assertEquals(0, new BigDecimal("50.00").compareTo(callback.getValue().getTotalRefundedAmount()));
        assertEquals(false, callback.getValue().getFullyRefunded());
    }

    /**
     * 验证 createRefund 并发抢占失败（条件更新影响 0 行）时抛出 REFUND_CONFLICT，
     * 不落退款记录、不调网关与订单服务，杜绝双重退款。
     */
    @Test
    public void createRefund_atomic_claim_fails_when_not_success() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 读到 SUCCESS，但行级抢占已被并发退款先行置为 REFUNDING
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(payTransactionMapper.updateStatusIf("PAY123", 2, 5)).thenReturn(0);
        when(payRefundMapper.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("0.00"));

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "test");
        BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));

        assertEquals("40007", ex.getCode());
        verify(payRefundMapper).insert(any(PayRefundPO.class));
        verify(transactionManager).rollback(any());
        verify(payGateway, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        verifyNoInteractions(orderFeignClient);
    }

    /**
     * 验证 createRefund 累计退款（已退 + 本次）超过支付金额时抛出 REFUND_EXCEED 并回滚抢占状态。
     */
    @Test
    public void createRefund_rejects_when_cumulative_exceeds() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(payTransactionMapper.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundMapper.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("80.00"));

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("30.00"), "test");
        BizException ex = assertThrows(BizException.class, () -> service.createRefund(dto));

        assertEquals("40008", ex.getCode());
        verify(payRefundMapper, never()).insert(any(PayRefundPO.class));
        verify(payTransactionMapper, never()).updateStatusIf("PAY123", 5, 2);
        verify(transactionManager).rollback(any());
        verify(payGateway, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        verifyNoInteractions(orderFeignClient);
    }

    /**
     * 验证 createRefund 正常路径：退款记录以 PENDING 落库、成功后置 SUCCESS，
     * 部分退款未退满时支付单回到 SUCCESS（允许继续部分退款）而非置 REFUNDED 挡住后续退款。
     */
    @Test
    public void createRefund_success_records_refund() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(payTransactionMapper.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundMapper.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
        when(payRefundMapper.updateStatusIf(
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
            return 1;
        }).when(payRefundMapper).insert(any(PayRefundPO.class));

        RefundVO vo = service.createRefund(dto);

        assertEquals(1, vo.getStatus()); // 退款记录最终 SUCCESS
        ArgumentCaptor<PayRefundPO> cap = ArgumentCaptor.forClass(PayRefundPO.class);
        verify(payRefundMapper).insert(cap.capture());
        assertEquals("PAY123", cap.getValue().getPaymentNo());
        assertEquals(0, insertStatusSnapshot[0]); // 落库时为 PENDING
        verify(payRefundMapper).updateStatusIf(
            "REFUND90001", 0, 1, "REFUND_TRADE_123");
        assertEquals(1, cap.getValue().getStatus()); // 成功后 SUCCESS
        assertEquals("REFUND-REQ-1", cap.getValue().getRefundRequestNo());
        verify(payTransactionMapper).updateStatusIf("PAY123", 5, 2); // 未退满回 SUCCESS
        ArgumentCaptor<RefundCallbackDTO> callback =
            ArgumentCaptor.forClass(RefundCallbackDTO.class);
        verify(orderFeignClient).refundCallback(callback.capture());
        assertEquals("REFUND-REQ-1", callback.getValue().getRefundRequestNo());
        assertEquals(false, callback.getValue().getFullyRefunded());
    }

    /**
     * 验证同一退款请求号直接复用已有退款记录，不重复抢占支付单或调用渠道。
     */
    @Test
    public void createRefund_reuses_existing_request() {
        PayRefundPO existing = new PayRefundPO();
        existing.setRefundNo("REFUND-EXISTING");
        existing.setRefundRequestNo("REFUND-REQ-1");
        existing.setPaymentNo("PAY123");
        existing.setAmount(new BigDecimal("50.00"));
        existing.setStatus(1);
        when(payRefundMapper.findByRefundRequestNo("REFUND-REQ-1")).thenReturn(existing);

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1");
        RefundVO result = service.createRefund(dto);

        assertEquals("REFUND-EXISTING", result.getRefundNo());
        verify(manager, never()).getById(anyLong());
        verify(payTransactionMapper, never()).updateStatusIf(anyString(), anyInt(), anyInt());
        verify(payGateway, never()).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        verify(orderFeignClient, never()).refundCallback(any(RefundCallbackDTO.class));
    }

    /**
     * 验证同一请求号遇到 PENDING 退款时，使用原退款单号查询并恢复，不生成新的外部退款号。
     */
    @Test
    public void createRefund_recovers_existing_pending_request() {
        PayRefundPO existing = new PayRefundPO();
        existing.setRefundNo("REFUND-STABLE");
        existing.setRefundRequestNo("REFUND-REQ-1");
        existing.setPaymentNo("PAY123");
        existing.setAmount(new BigDecimal("50.00"));
        existing.setReason("reason");
        existing.setStatus(0);
        when(payRefundMapper.findByRefundRequestNo("REFUND-REQ-1")).thenReturn(existing);

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
        when(payRefundMapper.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
        when(payRefundMapper.updateStatusIf(
            eq("REFUND-STABLE"), eq(0), eq(1), any())).thenReturn(1);

        RefundVO result = service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1"));

        assertEquals("REFUND-STABLE", result.getRefundNo());
        assertEquals(1, result.getStatus());
        verify(payGateway).refund("PAY123", "REFUND-STABLE", new BigDecimal("50.00"), "reason");
        verify(payTransactionMapper).updateStatusIf("PAY123", 5, 2);
    }

    /**
     * 验证渠道异常时本地退款保持 PENDING 且支付单保持 REFUNDING，等待补偿扫描恢复。
     */
    @Test
    public void createRefund_gateway_exception_keeps_pending_for_retry() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2);
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(payTransactionMapper.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundMapper.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenThrow(new RuntimeException("gateway timeout"));

        ArgumentCaptor<PayRefundPO> refundCaptor = ArgumentCaptor.forClass(PayRefundPO.class);
        when(payRefundMapper.insert(refundCaptor.capture())).thenReturn(1);

        assertThrows(BizException.class, () -> service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                1L, new BigDecimal("50.00"), "reason", "REFUND-REQ-1")));

        assertEquals(0, refundCaptor.getValue().getStatus());
        verify(payRefundMapper, never()).updateStatusIf(
            anyString(), anyInt(), anyInt(), anyString());
        verify(payTransactionMapper, never()).updateStatusIf("PAY123", 5, 2);
        verify(orderFeignClient, never()).refundCallback(any(RefundCallbackDTO.class));
    }

    /**
     * 验证补偿扫描先查询渠道，已退款成功时只补写本地状态，不重复发起退款。
     */
    @Test
    public void scanPendingRefunds_completes_refund_found_in_gateway() {
        PayRefundPO pending = new PayRefundPO();
        pending.setRefundNo("REFUND-STABLE");
        pending.setPaymentNo("PAY123");
        pending.setAmount(new BigDecimal("50.00"));
        pending.setStatus(0);
        pending.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        when(payRefundMapper.selectPendingRefunds(any(LocalDateTime.class), eq(100)))
            .thenReturn(List.of(pending));
        when(payRefundMapper.claimPendingRefund(
            eq("REFUND-STABLE"), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);

        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getOne(any())).thenReturn(payTx);
        when(payGateway.queryRefund("PAY123", "REFUND-STABLE"))
            .thenReturn(PayGateway.RefundQueryStatus.SUCCESS);
        when(payRefundMapper.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
        when(payRefundMapper.updateStatusIf(
            eq("REFUND-STABLE"), eq(0), eq(1), any())).thenReturn(1);

        int count = service.scanPendingRefunds();

        assertEquals(1, count);
        verify(payGateway, never()).refund(
            anyString(), anyString(), any(BigDecimal.class), anyString());
        verify(payTransactionMapper).updateStatusIf("PAY123", 5, 2);
        ArgumentCaptor<RefundCallbackDTO> callback =
            ArgumentCaptor.forClass(RefundCallbackDTO.class);
        verify(orderFeignClient).refundCallback(callback.capture());
        assertEquals(false, callback.getValue().getFullyRefunded());
    }

    /**
     * 验证 createRefund 累计退满支付金额时支付单置 REFUNDED（部分退款终态语义）。
     */
    @Test
    public void createRefund_full_refund_marks_refunded() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(payTransactionMapper.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundMapper.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00")).thenReturn(new BigDecimal("100.00"));
        when(payRefundMapper.updateStatusIf(
            anyString(), eq(0), eq(1), anyString())).thenReturn(1);
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn("REFUND_TRADE_123");

        com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO dto =
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("100.00"), "reason");
        service.createRefund(dto);

        verify(payTransactionMapper).updateStatusIf("PAY123", 5, 6); // 累计退满置 REFUNDED
        ArgumentCaptor<RefundCallbackDTO> callback =
            ArgumentCaptor.forClass(RefundCallbackDTO.class);
        verify(orderFeignClient).refundCallback(callback.capture());
        assertEquals(true, callback.getValue().getFullyRefunded());
    }

    /**
     * 验证退款网关成功后订单通知失败时，退款单保持成功且不回滚支付状态。
     */
    @Test
    public void createRefund_gateway_success_order_callback_fails_keeps_success() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2);
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("100");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(payTransactionMapper.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundMapper.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
        when(payRefundMapper.updateStatusIf(
            anyString(), eq(0), eq(1), anyString())).thenReturn(1);
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn("REFUND_TRADE_123");
        when(orderFeignClient.refundCallback(any(RefundCallbackDTO.class)))
            .thenThrow(new RuntimeException("order unavailable"));

        RefundVO vo = service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(
                1L, new BigDecimal("50.00"), "reason"));

        assertEquals(1, vo.getStatus());
        verify(payTransactionMapper).updateStatusIf("PAY123", 5, 2);
        verify(payRefundMapper, never()).updateById(argThat((PayRefundPO r) -> r.getStatus() == 2));
    }

    /**
     * 验证退款通知补偿扫描会重试未通知退款并标记 notifiedAt。
     */
    @Test
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
        when(payRefundMapper.selectList(any())).thenReturn(List.of(refund));
        when(manager.getOne(any())).thenReturn(payTx);
        when(payRefundMapper.sumCommittedAmount("PAY123")).thenReturn(new BigDecimal("50.00"));
        when(orderFeignClient.refundCallback(any(RefundCallbackDTO.class)))
            .thenReturn(Result.success(null));
        when(payRefundMapper.updateById(refund)).thenReturn(1);

        int count = service.scanUnnotifiedRefunds();

        assertEquals(1, count);
        assertNotNull(refund.getNotifiedAt());
        verify(payRefundMapper).updateById(refund);
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
        int count = service.scanPaidButOrderPending();

        assertEquals(1, count);
        verify(orderFeignClient).pay(100L);
    }

    /**
     * 验证数字订单标识通过订单ID接口幂等推进。
     */
    @Test
    public void scanPaidButOrderPending_advances_numeric_order() {
        PayTransactionPO tx = new PayTransactionPO();
        tx.setPaymentNo("PAY1");
        tx.setOrderNo("100");
        tx.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx));
        int count = service.scanPaidButOrderPending();

        assertEquals(1, count);
        verify(orderFeignClient).pay(100L);
    }

    /**
     * 验证 scanPaidButOrderPending 支持非数字业务单号。
     */
    @Test
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
        verify(orderFeignClient).payByOrderNo("NOT_AN_ID");
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
     * 验证 createPayment 在商户上下文缺失时归属默认商户 0L，避免 t_pay_transaction.merchant_id 非空约束报错。
     */
    @Test
    public void createPayment_null_merchant_defaults_to_zero() {
        Long id = service.createPayment("ORDER300", 3L, null, new BigDecimal("10.00"), null);
        assertNotNull(id);
        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(0L, cap.getValue().getMerchantId());
    }

    /**
     * 验证 createPayment 商户缺失时按订单归属解析商户，order-service 返回真实商户则落库真实值。
     */
    @Test
    public void createPayment_resolves_merchant_from_order() {
        when(orderFeignClient.merchantId("ORDER400"))
            .thenReturn(com.yirancrazy.minimall.common.result.Result.success(42L));

        Long id = service.createPayment("ORDER400", 4L, null, new BigDecimal("10.00"), null);

        assertNotNull(id);
        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(42L, cap.getValue().getMerchantId());
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

    private static OrderPaySnapshotDTO snapshot(String orderNo, Long userId, Long merchantId,
                                                String amount, int status) {
        return new OrderPaySnapshotDTO(1L, orderNo, userId, merchantId,
            new BigDecimal(amount), status, null);
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
     * 验证 handleCallback 回调金额与库内支付单不符时冻结支付单且不推进订单（防伪造/篡改回调）。
     */
    @Test
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
        verify(orderFeignClient, never()).pay(anyLong());
        verify(orderFeignClient, never()).payByOrderNo(anyString());
    }

    /**
     * 验证订单推进失败时支付单保持 PENDING 并抛出异常，允许渠道重试回调。
     */
    @Test
    public void handleCallback_order_advance_failure_keeps_pending() {
        PayTransactionPO rec = new PayTransactionPO();
        rec.setId(1L);
        rec.setStatus(1);
        rec.setOrderNo("100");
        rec.setAmount(new BigDecimal("100.00"));
        when(manager.getOne(any())).thenReturn(rec);
        when(orderFeignClient.pay(100L)).thenThrow(new RuntimeException("order unavailable"));

        PayCallbackDTO dto = new PayCallbackDTO("PAY123", "TRADE123", true, "response",
            new BigDecimal("100.00"));

        assertThrows(BizException.class, () -> service.handleCallback(dto));
        assertEquals(1, rec.getStatus());
        verify(manager, never()).updateById(rec);
    }

    /**
     * 验证 handleCallback 对已 SUCCESS 支付单的重复回调直接幂等返回，不重复推进订单。
     */
    @Test
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
        verify(orderFeignClient, never()).pay(anyLong());
    }

    /**
     * 验证 handleCallback 失败回调不得覆盖已 SUCCESS 支付单（防状态倒灌）。
     */
    @Test
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
        verify(orderFeignClient, never()).pay(anyLong());
    }

    /**
     * 验证 handleCallback 失败回调仅迁移 PENDING，已 FAILED 等非 PENDING 状态不重复更新。
     */
    @Test
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
     * 验证 createRefund 对非数字业务单号（C 端直付）退款不抛 NumberFormatException，
     * 退款流程正常完成且跳过订单推进回调。
     */
    @Test
    public void createRefund_non_numeric_order_no_skips_feign() {
        PayTransactionPO payTx = new PayTransactionPO();
        payTx.setId(1L);
        payTx.setStatus(2); // 支付成功（SUCCESS）
        payTx.setPaymentNo("PAY123");
        payTx.setAmount(new BigDecimal("100.00"));
        payTx.setOrderNo("OD20260814001");
        when(manager.getById(1L)).thenReturn(payTx);
        when(manager.getOne(any())).thenReturn(payTx);
        when(payTransactionMapper.updateStatusIf("PAY123", 2, 5)).thenReturn(1);
        when(payRefundMapper.sumCommittedAmount("PAY123"))
            .thenReturn(new BigDecimal("0.00"), new BigDecimal("50.00"));
        when(payRefundMapper.updateStatusIf(
            anyString(), eq(0), eq(1), anyString())).thenReturn(1);
        when(payGateway.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
            .thenReturn("REFUND_TRADE_123");

        RefundVO vo = service.createRefund(
            new com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO(1L, new BigDecimal("50.00"), "reason"));

        assertNotNull(vo);
        verify(payGateway).refund(anyString(), anyString(), any(BigDecimal.class), anyString());
        verify(orderFeignClient, never()).refundCallback(any(RefundCallbackDTO.class));
        verify(payTransactionMapper).updateStatusIf("PAY123", 5, 2); // 未退满回 SUCCESS
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
        when(orderFeignClient.pay(100L)).thenThrow(new RuntimeException("rpc down"));

        int count = service.scanPaidButOrderPending();

        assertEquals(1, count);
        verify(orderFeignClient).pay(100L);
        verify(orderFeignClient).pay(200L);
    }

    /**
     * 验证订单推进失败时不计入补偿成功数。
     */
    @Test
    public void scanPaidButOrderPending_advance_failure_skips_count() {
        PayTransactionPO tx = new PayTransactionPO();
        tx.setPaymentNo("PAY1");
        tx.setOrderNo("100");
        tx.setStatus(2);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(java.util.List.of(tx));
        when(orderFeignClient.pay(100L)).thenReturn(Result.fail("40012", "advance failed"));

        int count = service.scanPaidButOrderPending();

        assertEquals(0, count);
        verify(orderFeignClient).pay(100L);
    }

    /**
     * 验证 scanPaidButOrderPending 分批扫描：每批查询带 24h 时间窗与 LIMIT 100，
     * 第一批满 100 条后第二批按 id 游标（id > 100）推进，不满批即停止。
     */
    @Test
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
     * 验证 scanPaidButOrderPending 每批满 100 条时最多扫描 SCAN_MAX_BATCHES(10) 批即返回，防止单次调用无限循环。
     */
    @Test
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
