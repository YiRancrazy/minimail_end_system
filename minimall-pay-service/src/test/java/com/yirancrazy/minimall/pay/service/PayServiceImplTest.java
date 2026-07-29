package com.yirancrazy.minimall.pay.service;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.pay.entity.PayRecordPO;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.service.impl.PayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayServiceImpl 单元测试，使用 Mockito 模拟 PayManager，覆盖创建支付单、成功回调与缺失支付单三种场景。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public class PayServiceImplTest {

    private PayManager manager;
    private PayServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(PayManager.class);
        lenient().when(manager.updateById(any(PayRecordPO.class))).thenReturn(true);
        doAnswer(inv -> {
            PayRecordPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(manager).save(any(PayRecordPO.class));
        service = new PayServiceImpl(manager);
    }

    /**
     * 验证 create 方法会持久化一条状态为 PENDING 且金额正确的支付单记录。
     */
    @Test
    public void create_persists_pending_record() {
        Long id = service.create(100L, new BigDecimal("99.99"));
        assertNotNull(id);
        ArgumentCaptor<PayRecordPO> cap = ArgumentCaptor.forClass(PayRecordPO.class);
        org.mockito.Mockito.verify(manager).save(cap.capture());
        assertEquals("PENDING", cap.getValue().getStatus());
        assertEquals(0, new BigDecimal("99.99").compareTo(cap.getValue().getAmount()));
    }

    /**
     * 验证 callback 在支付成功时把支付单状态推进为 PAID 并返回 true。
     */
    @Test
    public void callback_marks_paid() {
        PayRecordPO rec = new PayRecordPO();
        rec.setId(1L);
        rec.setStatus("PENDING");
        when(manager.getOne(any())).thenReturn(rec);

        boolean ok = service.callback(1L, true);
        assertEquals(true, ok);
        assertEquals("PAID", rec.getStatus());
    }

    /**
     * 验证 callback 在找不到对应支付单时抛出 PAY_NOT_FOUND 业务异常。
     */
    @Test
    public void callback_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.callback(99L, true));
    }

    /**
     * 验证 callback 在支付失败时将支付单状态推进为 FAILED。
     */
    @Test
    public void callback_marks_failed() {
        PayRecordPO rec = new PayRecordPO();
        rec.setId(1L);
        rec.setStatus("PENDING");
        when(manager.getOne(any())).thenReturn(rec);

        boolean ok = service.callback(1L, false);
        assertEquals(true, ok);
        assertEquals("FAILED", rec.getStatus());
    }

    /**
     * 验证 create 在金额为 null 时按 0 处理。
     */
    @Test
    public void create_with_null_amount() {
        Long id = service.create(100L, null);
        assertNotNull(id);
        ArgumentCaptor<PayRecordPO> cap = ArgumentCaptor.forClass(PayRecordPO.class);
        org.mockito.Mockito.verify(manager).save(cap.capture());
        assertEquals(0, BigDecimal.ZERO.compareTo(cap.getValue().getAmount()));
    }
}