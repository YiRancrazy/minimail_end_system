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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockServiceImpl 单元测试，基于 Mockito 打桩 StockManager，
 *               覆盖库存预占成功与库存不足抛出 BizException、以及预占释放后可用量回补的场景。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public class StockServiceImplTest {

    private StockManager manager;
    private StockServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(StockManager.class);
        lenient().when(manager.updateById(any(StockPO.class))).thenReturn(true);
        service = new StockServiceImpl(manager);
    }

    /**
     * 验证预占库存时按数量扣减可用量并等额增加预占量，且在可用量不足时抛出 BizException。
     */
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

    /**
     * 验证释放预占库存时按数量扣减预占量并等额回补可用量，且返回释放成功。
     */
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

    /**
     * 验证预占不存在的 SKU 时抛出 STOCK_NOT_FOUND 业务异常。
     */
    @Test
    public void reserve_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.reserve(999L, 1));
    }

    /**
     * 验证释放不存在的 SKU 库存时返回 false 而非抛出异常。
     */
    @Test
    public void release_missing_returns_false() {
        when(manager.getOne(any())).thenReturn(null);
        boolean ok = service.release(999L, 1);
        assertEquals(false, ok);
    }

    /**
     * 验证释放数量超过预占数量时返回 false。
     */
    @Test
    public void release_exceeds_reserved_returns_false() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(0L);
        s.setReserved(2L);
        when(manager.getOne(any())).thenReturn(s);

        boolean ok = service.release(100L, 5);
        assertEquals(false, ok);
    }

    /**
     * 验证查询存在的 SKU 库存返回正确的可用数量。
     */
    @Test
    public void query_returns_available() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(2L);
        when(manager.getOne(any())).thenReturn(s);

        long available = service.query(100L);
        assertEquals(10L, available);
    }

    /**
     * 验证查询不存在的 SKU 库存返回 0。
     */
    @Test
    public void query_missing_returns_zero() {
        when(manager.getOne(any())).thenReturn(null);
        long available = service.query(999L);
        assertEquals(0L, available);
    }
}