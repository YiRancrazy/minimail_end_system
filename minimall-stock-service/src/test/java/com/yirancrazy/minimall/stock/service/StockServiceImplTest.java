package com.yirancrazy.minimall.stock.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.stock.constant.StockCountTaskStatusEnum;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDTO;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDTO;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;
import com.yirancrazy.minimall.stock.manager.StockCountTaskManager;
import com.yirancrazy.minimall.stock.manager.StockJournalManager;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.manager.StockTransferManager;
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
    private StockTransferManager transferManager;
    private StockCountTaskManager countTaskManager;
    private StockServiceImpl service;

    @BeforeEach
    void setUp() {
        // Initialize TableInfo so LambdaQueryWrapper can resolve lambda cache
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.stock.mapper.StockMapper");
        TableInfoHelper.initTableInfo(assistant, StockPO.class);
        TableInfoHelper.initTableInfo(assistant, StockJournalPO.class);

        manager = mock(StockManager.class);
        journalManager = mock(StockJournalManager.class);
        stockMapper = mock(StockMapper.class);
        transferManager = mock(StockTransferManager.class);
        countTaskManager = mock(StockCountTaskManager.class);
        lenient().when(manager.updateById(any(StockPO.class))).thenReturn(true);
        lenient().when(journalManager.save(any(StockJournalPO.class))).thenReturn(true);
        lenient().when(transferManager.save(any(StockTransferPO.class))).thenReturn(true);
        lenient().when(countTaskManager.updateById(any(StockCountTaskPO.class))).thenReturn(true);
        service = new StockServiceImpl(manager, journalManager, stockMapper,
            transferManager, countTaskManager);
    }

    /**
     * 验证预占成功时原子扣减命中 1 行并记录流水。
     */
    @Test
    public void reserve_atomic_success_records_journal() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(stockMapper.deductAvailable(100L, 2L)).thenReturn(1);

        boolean ok = service.reserve(100L, 2);
        assertEquals(true, ok);
        assertEquals(8L, s.getAvailable());
        assertEquals(2L, s.getReserved());
        verify(stockMapper).deductAvailable(100L, 2L);
        verify(journalManager).save(any(StockJournalPO.class));
    }

    /**
     * 验证原子扣减未命中（0 行）时抛出 STOCK_INSUFFICIENT 且不记录流水。
     */
    @Test
    public void reserve_insufficient_stock_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(3L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(stockMapper.deductAvailable(100L, 100L)).thenReturn(0);

        assertThrows(BizException.class, () -> service.reserve(100L, 100));
        verify(journalManager, never()).save(any(StockJournalPO.class));
    }

    /**
     * 验证预占数量非法（null 或 <= 0）时抛出 BizException。
     */
    @Test
    public void reserve_invalid_quantity_throws() {
        assertThrows(BizException.class, () -> service.reserve(100L, null));
        assertThrows(BizException.class, () -> service.reserve(100L, 0));
        assertThrows(BizException.class, () -> service.reserve(100L, -1));
    }

    /**
     * 验证释放预占库存时原子回补命中 1 行并记录流水。
     */
    @Test
    public void release_restores_stock() {
        when(stockMapper.restoreReserved(100L, 3L)).thenReturn(1);

        boolean ok = service.release(100L, 3);
        assertEquals(true, ok);
        verify(stockMapper).restoreReserved(100L, 3L);
        verify(journalManager).save(any(StockJournalPO.class));
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
     * 验证释放不存在的 SKU 库存时原子释放未命中（0 行）返回 false 而非抛出异常。
     */
    @Test
    public void release_missing_returns_false() {
        when(stockMapper.restoreReserved(999L, 1L)).thenReturn(0);
        boolean ok = service.release(999L, 1);
        assertEquals(false, ok);
    }

    /**
     * 验证释放数量超过预占数量（原子释放未命中）时返回 false。
     */
    @Test
    public void release_insufficient_reserved_returns_false() {
        when(stockMapper.restoreReserved(100L, 5L)).thenReturn(0);

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
     * 商家视角查询：库存归属与当前商家一致时返回可用数量。
     */
    @Test
    public void query_merchant_owned_returns_available() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        long available = service.query(100L, 1L);
        assertEquals(10L, available);
    }

    /**
     * F13: 商家视角查询其他商家 SKU 库存时抛出 STOCK_NOT_FOUND，防止越权读取。
     */
    @Test
    public void query_merchant_mismatch_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.query(100L, 2L));
    }

    /**
     * 验证 page 委托给 manager.list 并返回游标分页结果。
     */
    @Test
    public void page_delegates_to_manager() {
        StockPageDTO dto = new StockPageDTO();
        List<StockPO> mockRecords = new ArrayList<>();
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(mockRecords);

        CursorPageVO<StockPO> result = service.page(dto);
        assertNotNull(result);
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
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

    /**
     * 验证调拨成功时扣减源库存、增加目标库存、记录调拨流水与调拨记录。
     */
    @Test
    public void transfer_success_decrements_and_increments() {
        StockPO from = new StockPO();
        from.setId(1L);
        from.setSkuId(100L);
        from.setAvailable(10L);
        from.setReserved(0L);
        StockPO to = new StockPO();
        to.setId(2L);
        to.setSkuId(200L);
        to.setAvailable(5L);
        to.setReserved(0L);
        when(manager.getOne(any())).thenReturn(from, to);
        when(stockMapper.deductAvailable(100L, 3L)).thenReturn(1);
        when(stockMapper.increaseAvailable(200L, 3L)).thenReturn(1);

        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromSkuId(100L);
        dto.setToSkuId(200L);
        dto.setQuantity(3L);
        dto.setReason("test");
        dto.setOperatorId(1L);
        service.transfer(dto);

        assertEquals(7L, from.getAvailable());
        assertEquals(8L, to.getAvailable());
        verify(stockMapper).deductAvailable(100L, 3L);
        verify(stockMapper).increaseAvailable(200L, 3L);
        verify(transferManager).save(any(StockTransferPO.class));
    }

    /**
     * 验证调拨相同SKU时抛出 BizException。
     */
    @Test
    public void transfer_same_sku_throws() {
        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromSkuId(100L);
        dto.setToSkuId(100L);
        dto.setQuantity(1L);
        assertThrows(BizException.class, () -> service.transfer(dto));
    }

    /**
     * 验证源库存不足时调拨抛出 BizException。
     */
    @Test
    public void transfer_insufficient_throws() {
        StockPO from = new StockPO();
        from.setId(1L);
        from.setSkuId(100L);
        from.setAvailable(2L);
        from.setReserved(0L);
        when(manager.getOne(any())).thenReturn(from);

        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromSkuId(100L);
        dto.setToSkuId(200L);
        dto.setQuantity(5L);
        assertThrows(BizException.class, () -> service.transfer(dto));
    }

    /**
     * 验证目标SKU库存不存在时调拨抛出 BizException。
     */
    @Test
    public void transfer_target_not_found_throws() {
        StockPO from = new StockPO();
        from.setId(1L);
        from.setSkuId(100L);
        from.setAvailable(10L);
        from.setReserved(0L);
        when(manager.getOne(any())).thenReturn(from, null);

        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromSkuId(100L);
        dto.setToSkuId(200L);
        dto.setQuantity(3L);
        assertThrows(BizException.class, () -> service.transfer(dto));
    }

    /**
     * 验证 transferPage 委托给 transferManager.list 并返回游标分页结果。
     */
    @Test
    public void transferPage_delegates_to_manager() {
        StockTransferPageDTO dto = new StockTransferPageDTO();
        List<StockTransferPO> mockRecords = new ArrayList<>();
        when(transferManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(mockRecords);

        CursorPageVO<StockTransferPO> result = service.transferPage(dto);
        assertNotNull(result);
        verify(transferManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证创建盘点任务时查询当前可用库存作为期望数量。
     */
    @Test
    public void createCountTask_succeeds() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        StockCountTaskCreateDTO dto = new StockCountTaskCreateDTO();
        dto.setSkuId(100L);
        dto.setOperatorId(1L);
        service.createCountTask(dto);

        verify(countTaskManager).save(any(StockCountTaskPO.class));
    }

    /**
     * 验证完成盘点任务有差异时调整库存并推进状态为 COMPLETED。
     */
    @Test
    public void completeCountTask_with_diff_adjusts_stock() {
        StockCountTaskPO task = new StockCountTaskPO();
        task.setId(1L);
        task.setSkuId(100L);
        task.setExpectedQuantity(10L);
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);
        when(countTaskManager.completeIfStatus(eq(1L), anyInt(), anyInt(), any(), any(), any()))
            .thenReturn(1);

        StockPO s = new StockPO();
        s.setId(2L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        StockCountTaskCompleteDTO dto = new StockCountTaskCompleteDTO();
        dto.setActualQuantity(8L);
        service.completeCountTask(1L, dto);

        assertEquals(StockCountTaskStatusEnum.COMPLETED.intCode(), task.getStatus());
        assertEquals(-2L, task.getDiffQuantity());
        verify(countTaskManager).completeIfStatus(eq(1L),
            eq(StockCountTaskStatusEnum.PENDING.intCode()),
            eq(StockCountTaskStatusEnum.COMPLETED.intCode()), eq(8L), eq(-2L), any());
        verify(manager).updateById(any(StockPO.class));
    }

    /**
     * 验证完成盘点任务无差异时不调整库存。
     */
    @Test
    public void completeCountTask_no_diff_succeeds() {
        StockCountTaskPO task = new StockCountTaskPO();
        task.setId(1L);
        task.setSkuId(100L);
        task.setExpectedQuantity(10L);
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);
        when(countTaskManager.completeIfStatus(eq(1L), anyInt(), anyInt(), any(), any(), any()))
            .thenReturn(1);

        StockCountTaskCompleteDTO dto = new StockCountTaskCompleteDTO();
        dto.setActualQuantity(10L);
        service.completeCountTask(1L, dto);

        assertEquals(0L, task.getDiffQuantity());
        verify(manager, never()).updateById(any(StockPO.class));
    }

    /**
     * F28: 验证并发完成时条件更新未命中（影响 0 行）抛出 BizException 且盘点差异不重复入账。
     */
    @Test
    public void completeCountTask_concurrent_update_miss_throws() {
        StockCountTaskPO task = new StockCountTaskPO();
        task.setId(1L);
        task.setSkuId(100L);
        task.setExpectedQuantity(10L);
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);
        when(countTaskManager.completeIfStatus(eq(1L), anyInt(), anyInt(), any(), any(), any()))
            .thenReturn(0);

        StockCountTaskCompleteDTO dto = new StockCountTaskCompleteDTO();
        dto.setActualQuantity(8L);
        assertThrows(BizException.class, () -> service.completeCountTask(1L, dto));
        // 条件更新未命中：不触发库存调整与流水记录
        verify(manager, never()).getOne(any());
        verify(manager, never()).updateById(any(StockPO.class));
        verify(journalManager, never()).save(any(StockJournalPO.class));
    }

    /**
     * 验证完成非PENDING状态的盘点任务时抛出 BizException。
     */
    @Test
    public void completeCountTask_not_pending_throws() {
        StockCountTaskPO task = new StockCountTaskPO();
        task.setId(1L);
        task.setStatus(StockCountTaskStatusEnum.COMPLETED.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);

        StockCountTaskCompleteDTO dto = new StockCountTaskCompleteDTO();
        dto.setActualQuantity(10L);
        assertThrows(BizException.class, () -> service.completeCountTask(1L, dto));
    }

    /**
     * 验证取消PENDING状态的盘点任务时推进为 CANCELLED。
     */
    @Test
    public void cancelCountTask_succeeds() {
        StockCountTaskPO task = new StockCountTaskPO();
        task.setId(1L);
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);
        when(countTaskManager.cancelIfStatus(1L, StockCountTaskStatusEnum.PENDING.intCode(),
            StockCountTaskStatusEnum.CANCELLED.intCode())).thenReturn(1);

        service.cancelCountTask(1L, 99L);
        assertEquals(StockCountTaskStatusEnum.CANCELLED.intCode(), task.getStatus());
        verify(countTaskManager).cancelIfStatus(1L, StockCountTaskStatusEnum.PENDING.intCode(),
            StockCountTaskStatusEnum.CANCELLED.intCode());
    }

    /**
     * 验证取消非PENDING状态的盘点任务时抛出 BizException。
     */
    @Test
    public void cancelCountTask_not_pending_throws() {
        StockCountTaskPO task = new StockCountTaskPO();
        task.setId(1L);
        task.setStatus(StockCountTaskStatusEnum.CANCELLED.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);

        assertThrows(BizException.class, () -> service.cancelCountTask(1L, 99L));
    }

    /**
     * 验证 countTaskPage 委托给 countTaskManager.list 并返回游标分页结果。
     */
    @Test
    public void countTaskPage_delegates_to_manager() {
        StockCountTaskPageDTO dto = new StockCountTaskPageDTO();
        List<StockCountTaskPO> mockRecords = new ArrayList<>();
        when(countTaskManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(mockRecords);

        CursorPageVO<StockCountTaskPO> result = service.countTaskPage(dto);
        assertNotNull(result);
        verify(countTaskManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证创建盘点任务时SKU库存不存在抛出 BizException。
     */
    @Test
    public void createCountTask_stock_not_found_throws() {
        when(manager.getOne(any())).thenReturn(null);

        StockCountTaskCreateDTO dto = new StockCountTaskCreateDTO();
        dto.setSkuId(999L);
        dto.setOperatorId(1L);
        assertThrows(BizException.class, () -> service.createCountTask(dto));
    }

    /**
     * 验证 listAbnormalStock 返回 available &lt; 0 或 reserved &lt; 0 的记录。
     */
    @Test
    public void listAbnormalStock_returns_abnormal_records() {
        StockPO abnormal = new StockPO();
        abnormal.setId(1L);
        abnormal.setSkuId(100L);
        abnormal.setAvailable(-5L);
        abnormal.setReserved(0L);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
            .thenReturn(List.of(abnormal));

        List<StockPO> result = service.listAbnormalStock();
        assertEquals(1, result.size());
        assertEquals(-5L, result.get(0).getAvailable());
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证纠正库存成功时更新可用量并记录流水。
     */
    @Test
    public void correctStock_succeeds() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        service.correctStock(100L, 20L, "data error");

        assertEquals(20L, s.getAvailable());
        verify(manager).updateById(s);
        verify(journalManager).save(any(StockJournalPO.class));
    }

    /**
     * 验证纠正库存时数量为负抛出 BizException。
     */
    @Test
    public void correctStock_negative_quantity_throws() {
        assertThrows(BizException.class, () -> service.correctStock(100L, -1L, "test"));
    }

    /**
     * 验证纠正库存时SKU不存在抛出 BizException。
     */
    @Test
    public void correctStock_stock_not_found_throws() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.correctStock(999L, 10L, "test"));
    }

    /**
     * 验证 adjustStock 正数调整成功时增加可用量并记录流水。
     */
    @Test
    public void adjustStock_positive_increments_available() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        service.adjustStock(100L, 5L, "replenish");

        assertEquals(15L, s.getAvailable());
        verify(manager).updateById(s);
        verify(journalManager).save(any(StockJournalPO.class));
    }

    /**
     * 验证 adjustStock 负数调整成功时扣减可用量。
     */
    @Test
    public void adjustStock_negative_decrements_available() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        service.adjustStock(100L, -3L, "correction");

        assertEquals(7L, s.getAvailable());
        verify(manager).updateById(s);
    }

    /**
     * F53: 验证负向调整超过可用库存（调整后可用为负）时抛出 BizException 且不更新库存。
     */
    @Test
    public void adjustStock_negative_below_zero_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(3L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.adjustStock(100L, -5L, "correction"));
        verify(manager, never()).updateById(any(StockPO.class));
        verify(journalManager, never()).save(any(StockJournalPO.class));
    }

    /**
     * 验证 adjustStock 调整数量为 null 时抛出 BizException。
     */
    @Test
    public void adjustStock_null_quantity_throws() {
        assertThrows(BizException.class, () -> service.adjustStock(100L, null, "test"));
    }

    /**
     * 验证 adjustStock 在库存记录不存在时自动创建初始记录（可用 0）再累加调整量。
     */
    @Test
    public void adjustStock_noRecord_createsThenIncrements() {
        final StockPO[] created = new StockPO[1];
        when(manager.getOne(any())).thenReturn(null);
        when(manager.save(any(StockPO.class))).thenAnswer(invocation -> {
            StockPO po = invocation.getArgument(0);
            po.setId(2L);
            created[0] = po;
            return true;
        });

        service.adjustStock(200L, 5L, "initial");

        assertNotNull(created[0]);
        assertEquals(200L, created[0].getSkuId());
        assertEquals(5L, created[0].getAvailable());
        verify(manager).save(created[0]);
        verify(manager).updateById(created[0]);
        verify(journalManager).save(any(StockJournalPO.class));
    }

    /**
     * F13: 商家视角调整其他商家 SKU 库存时抛出 STOCK_NOT_FOUND，防止越权改库存。
     */
    @Test
    public void adjustStock_merchant_mismatch_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.adjustStock(100L, 5L, "replenish", 2L));
        verify(manager, never()).updateById(any(StockPO.class));
    }

    /**
     * F13: 商家视角调整库存遇无归属（merchant_id 为空）存量记录时抛出 STOCK_NOT_FOUND，不允许认领。
     */
    @Test
    public void adjustStock_merchant_null_ownership_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.adjustStock(100L, 5L, "replenish", 1L));
        verify(manager, never()).updateById(any(StockPO.class));
    }

    /**
     * F13: 商家视角调整库存，记录不存在时以该商家归属创建并写入 merchantId。
     */
    @Test
    public void adjustStock_merchant_noRecord_creates_with_merchantId() {
        final StockPO[] created = new StockPO[1];
        when(manager.getOne(any())).thenReturn(null);
        when(manager.save(any(StockPO.class))).thenAnswer(invocation -> {
            StockPO po = invocation.getArgument(0);
            po.setId(2L);
            created[0] = po;
            return true;
        });

        service.adjustStock(200L, 5L, "initial", 7L);

        assertNotNull(created[0]);
        assertEquals(7L, created[0].getMerchantId());
        assertEquals(5L, created[0].getAvailable());
        verify(manager).save(created[0]);
    }

    /**
     * 验证 setThreshold 在库存记录不存在时自动创建初始记录。
     */
    @Test
    public void setThreshold_noRecord_creates() {
        final StockPO[] created = new StockPO[1];
        when(manager.getOne(any())).thenReturn(null);
        when(manager.save(any(StockPO.class))).thenAnswer(invocation -> {
            StockPO po = invocation.getArgument(0);
            po.setId(3L);
            created[0] = po;
            return true;
        });

        service.setThreshold(300L, 20L);

        assertNotNull(created[0]);
        assertEquals(300L, created[0].getSkuId());
        assertEquals(20L, created[0].getAlertThreshold());
        verify(manager).updateById(created[0]);
    }

    /**
     * 验证 setThreshold 正常设置预警阈值。
     */
    @Test
    public void setThreshold_succeeds() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        service.setThreshold(100L, 5L);

        assertEquals(5L, s.getAlertThreshold());
        verify(manager).updateById(s);
    }

    /**
     * 验证 setThreshold 阈值为 null 时抛出 BizException。
     */
    @Test
    public void setThreshold_null_throws() {
        assertThrows(BizException.class, () -> service.setThreshold(100L, null));
    }

    /**
     * 验证 setThreshold 阈值为负数时抛出 BizException。
     */
    @Test
    public void setThreshold_negative_throws() {
        assertThrows(BizException.class, () -> service.setThreshold(100L, -1L));
    }

    /**
     * F13: 商家视角设置阈值遇归属不匹配时抛出 STOCK_NOT_FOUND，防止越权改其他商家阈值。
     */
    @Test
    public void setThreshold_merchant_mismatch_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.setThreshold(100L, 5L, 2L));
        verify(manager, never()).updateById(any(StockPO.class));
    }

    /**
     * 验证 queryJournal 委托给 journalManager.list 并返回结果。
     */
    @Test
    public void queryJournal_delegates_to_journalManager() {
        StockJournalPO po = new StockJournalPO();
        po.setId(1L);
        po.setSkuId(100L);
        po.setQuantity(-2L);
        when(journalManager.list(any(Wrapper.class)))
            .thenReturn(List.of(po));

        List<StockJournalPO> result = service.queryJournal(100L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(journalManager).list(any(Wrapper.class));
    }

    /**
     * F54: 验证 queryJournal 传入的查询被限制在 200 条上限。
     */
    @Test
    public void queryJournal_applies_limit_200() {
        when(journalManager.list(any(Wrapper.class))).thenReturn(new ArrayList<>());

        service.queryJournal(100L);

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(journalManager).list(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().contains("LIMIT 200"));
    }

    /**
     * F13: 商家视角查询流水遇归属不匹配时抛出 STOCK_NOT_FOUND，防止越权读取流水。
     */
    @Test
    public void queryJournal_merchant_mismatch_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.queryJournal(100L, 2L));
        verify(journalManager, never()).list(any(Wrapper.class));
    }

    /**
     * F54: 验证 listAbnormalStock 传入的查询被限制在 200 条上限。
     */
    @Test
    public void listAbnormalStock_applies_limit_200() {
        when(manager.list(any(Wrapper.class))).thenReturn(new ArrayList<>());

        service.listAbnormalStock();

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(manager).list(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().contains("LIMIT 200"));
    }

    /**
     * 验证 completeCountTask 在任务不存在时抛出 BizException。
     */
    @Test
    public void completeCountTask_not_found_throws() {
        when(countTaskManager.getById(999L)).thenReturn(null);

        StockCountTaskCompleteDTO dto = new StockCountTaskCompleteDTO();
        dto.setActualQuantity(10L);
        assertThrows(BizException.class, () -> service.completeCountTask(999L, dto));
    }

    /**
     * 验证 cancelCountTask 在任务不存在时抛出 BizException。
     */
    @Test
    public void cancelCountTask_not_found_throws() {
        when(countTaskManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.cancelCountTask(999L, 99L));
    }
}