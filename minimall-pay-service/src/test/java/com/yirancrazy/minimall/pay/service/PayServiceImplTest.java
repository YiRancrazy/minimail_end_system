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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import com.yirancrazy.minimall.pay.service.impl.PayServiceImpl;


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
    private OrderFeignClient orderFeignClient;
    private PayServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(PayManager.class);
        alipayGateway = mock(AlipayGateway.class);
        payRefundMapper = mock(PayRefundMapper.class);
        orderFeignClient = mock(OrderFeignClient.class);
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
        service = new PayServiceImpl(manager, alipayGateway, payRefundMapper, orderFeignClient);
    }

    /**
     * 验证 createPayment 方法会持久化一条状态为 PENDING 且金额正确的支付单记录。
     */
    @Test
    public void createPayment_persists_pending_record() {
        Long id = service.createPayment("ORDER100", 1L, 1L, new BigDecimal("99.99"));
        assertNotNull(id);
        ArgumentCaptor<PayTransactionPO> cap = ArgumentCaptor.forClass(PayTransactionPO.class);
        verify(manager).save(cap.capture());
        assertEquals(1, cap.getValue().getStatus());
        assertEquals(0, new BigDecimal("99.99").compareTo(cap.getValue().getAmount()));
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
}