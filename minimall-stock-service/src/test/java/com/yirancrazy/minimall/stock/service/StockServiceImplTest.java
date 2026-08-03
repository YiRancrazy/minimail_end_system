package com.yirancrazy.minimall.stock.service;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockJournalManager;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.mapper.StockMapper;
import com.yirancrazy.minimall.stock.service.impl.StockServiceImpl;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;

/**
* StockServiceImpl 单元测试，基于 Mockito 打桩 StockManager，
 *               覆盖库存预占成功与库存不足抛出 BizException、以及预占释放后可用量回补的场景。
 */
public class StockServiceImplTest {

    private StockManager manager;
    private StockJournalManager journalManager;
    private StockMapper stockMapper;
    private StockServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(StockManager.class);
        journalManager = mock(StockJournalManager.class);
        stockMapper = mock(StockMapper.class);
        lenient().when(manager.updateById(any(StockPO.class))).thenReturn(true);
        lenient().when(journalManager.save(any(StockJournalPO.class))).thenReturn(true);
        service = new StockServiceImpl(manager, journalManager, stockMapper);
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

    /**
     * 验证 page 委托给 manager.page 并返回其结果。
     */
    @Test
    public void page_delegates_to_manager() {
        StockPageDTO dto = new StockPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        IPage<StockPO> expected = new Page<>(1, 10);
        when(manager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<StockPO> result = service.page(dto);
        assertEquals(expected, result);
        verify(manager).page(any(IPage.class), any());
    }

    /**
     * 验证 platformStatistics 委托 mapper 并计算预警比例（3/10=0.30）。
     */
    @Test
    public void platformStatistics_computes_alert_ratio() {
        StockStatisticsVO stat = new StockStatisticsVO(10L, 1000L, 200L, 3L, null);
        when(stockMapper.statistics()).thenReturn(stat);

        StockStatisticsVO result = service.platformStatistics();
        assertEquals(10L, result.getTotalSkuCount());
        assertEquals(1000L, result.getTotalAvailable());
        assertEquals(200L, result.getTotalReserved());
        assertEquals(3L, result.getAlertSkuCount());
        assertEquals(new BigDecimal("0.30"), result.getAlertRatio());
    }

    /**
     * 验证 platformStatistics 在 mapper 返回 null 时回退零值。
     */
    @Test
    public void platformStatistics_null_returns_zero() {
        when(stockMapper.statistics()).thenReturn(null);

        StockStatisticsVO result = service.platformStatistics();
        assertEquals(0L, result.getTotalSkuCount());
        assertEquals(BigDecimal.ZERO, result.getAlertRatio());
    }

    /**
     * 验证 exportJournal 委托 journalManager.list 并返回结果。
     */
    @Test
    public void exportJournal_delegates_to_journalManager() {
        StockJournalPO po = new StockJournalPO();
        po.setId(1L);
        po.setSkuId(100L);
        po.setQuantity(-2L);
        when(journalManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(List.of(po));

        List<StockJournalPO> result = service.exportJournal(100L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(journalManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
}