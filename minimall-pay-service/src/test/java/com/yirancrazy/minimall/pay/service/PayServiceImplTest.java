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

    @Test
    public void create_persists_pending_record() {
        Long id = service.create(100L, new BigDecimal("99.99"));
        assertNotNull(id);
        ArgumentCaptor<PayRecordPO> cap = ArgumentCaptor.forClass(PayRecordPO.class);
        org.mockito.Mockito.verify(manager).save(cap.capture());
        assertEquals("PENDING", cap.getValue().getStatus());
        assertEquals(0, new BigDecimal("99.99").compareTo(cap.getValue().getAmount()));
    }

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

    @Test
    public void callback_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.callback(99L, true));
    }
}