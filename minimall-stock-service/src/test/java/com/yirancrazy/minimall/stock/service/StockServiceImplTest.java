package com.yirancrazy.minimall.stock.service;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.service.impl.StockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StockServiceImplTest {

    private StockManager manager;
    private StockServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(StockManager.class);
        lenient().when(manager.updateById(any(StockPO.class))).thenReturn(true);
        service = new StockServiceImpl(manager);
    }

    @Test
    public void reserve_decrements_available_and_throws_when_insufficient() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(3L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        boolean ok = service.reserve(100L, 2);
        assertEquals(true, ok);
        assertEquals(1L, s.getAvailable());
        assertEquals(2L, s.getReserved());

        when(manager.getOne(any())).thenReturn(s);
        assertThrows(BizException.class, () -> service.reserve(100L, 100));
    }

    @Test
    public void release_restores_available() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(0L);
        s.setReserved(5L);
        when(manager.getOne(any())).thenReturn(s);

        boolean ok = service.release(100L, 3);
        assertEquals(true, ok);
        assertEquals(3L, s.getAvailable());
        assertEquals(2L, s.getReserved());
    }
}