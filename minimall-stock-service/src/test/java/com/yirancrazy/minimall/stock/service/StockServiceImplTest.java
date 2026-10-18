package com.yirancrazy.minimall.stock.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.stock.constant.StockCountTaskStatusEnum;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDTO;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.dto.StockStatisticsDTO;
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
import com.yirancrazy.minimall.stock.service.impl.StockServiceImpl;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class StockServiceImplTest {

    private StockManager manager;
    private StockJournalManager journalManager;
    private StockTransferManager transferManager;
    private StockCountTaskManager countTaskManager;
    private StockServiceImpl service;

    @BeforeEach
    void setUp() {
        // 初始化 TableInfo，便于 LambdaQueryWrapper 解析 lambda 缓存
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.stock.mapper.StockMapper");
        TableInfoHelper.initTableInfo(assistant, StockPO.class);
        TableInfoHelper.initTableInfo(assistant, StockJournalPO.class);

        manager = mock(StockManager.class);
        journalManager = mock(StockJournalManager.class);
        transferManager = mock(StockTransferManager.class);
        countTaskManager = mock(StockCountTaskManager.class);
        lenient().when(manager.updateById(any(StockPO.class))).thenReturn(true);
        lenient().when(journalManager.save(any(StockJournalPO.class))).thenReturn(true);
        lenient().when(transferManager.save(any(StockTransferPO.class))).thenReturn(true);
        lenient().when(countTaskManager.updateById(any(StockCountTaskPO.class))).thenReturn(true);
        service = new StockServiceImpl(manager, journalManager, transferManager, countTaskManager);
    }


    /**
     * 执行 reserve_atomic_success_records_journal 方法。
     */
    public void reserve_atomic_success_records_journal() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(manager.deductAvailable(100L, 2L)).thenReturn(1);

        boolean ok = service.reserve(100L, 2);
        assertEquals(true, ok);
        assertEquals(8L, s.getAvailable());
        assertEquals(2L, s.getReserved());
        verify(manager).deductAvailable(100L, 2L);
        verify(journalManager).save(any(StockJournalPO.class));
    }


    /**
     * 执行 reserve_insufficient_stock_throws 方法。
     */
    public void reserve_insufficient_stock_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(3L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(manager.deductAvailable(100L, 100L)).thenReturn(0);

        assertThrows(BizException.class, () -> service.reserve(100L, 100));
        verify(journalManager, never()).save(any(StockJournalPO.class));
    }


    /**
     * 执行 reserve_invalid_quantity_throws 方法。
     */
    public void reserve_invalid_quantity_throws() {
        assertThrows(BizException.class, () -> service.reserve(100L, null));
        assertThrows(BizException.class, () -> service.reserve(100L, 0));
        assertThrows(BizException.class, () -> service.reserve(100L, -1));
    }


    /**
     * 执行 reserveBatch_merges_duplicate_skus 方法。
     */
    public void reserveBatch_merges_duplicate_skus() {
        StockPO stock = new StockPO();
        stock.setId(1L);
        stock.setSkuId(100L);
        stock.setAvailable(10L);
        stock.setReserved(0L);
        when(manager.getOne(any())).thenReturn(stock);
        when(manager.deductAvailable(100L, 5L)).thenReturn(1);

        boolean ok = service.reserveBatch(List.of(
            new StockReserveDTO(100L, 2),
            new StockReserveDTO(100L, 3)));

        assertTrue(ok);
        verify(manager).deductAvailable(100L, 5L);
        verify(journalManager, times(1)).save(any(StockJournalPO.class));
    }


    /**
     * 执行 reserveBatch_any_insufficient_throws 方法。
     */
    public void reserveBatch_any_insufficient_throws() {
        StockPO first = new StockPO();
        first.setId(1L);
        first.setSkuId(100L);
        first.setAvailable(10L);
        first.setReserved(0L);
        StockPO second = new StockPO();
        second.setId(2L);
        second.setSkuId(200L);
        second.setAvailable(0L);
        second.setReserved(0L);
        when(manager.getOne(any())).thenReturn(first, second);
        when(manager.deductAvailable(100L, 1L)).thenReturn(1);
        when(manager.deductAvailable(200L, 1L)).thenReturn(0);

        assertThrows(BizException.class, () -> service.reserveBatch(List.of(
            new StockReserveDTO(100L, 1),
            new StockReserveDTO(200L, 1))));

        verify(manager).deductAvailable(100L, 1L);
        verify(manager).deductAvailable(200L, 1L);
    }


    /**
     * 执行 reserveBatch_rejects_more_than_100_skus 方法。
     */
    public void reserveBatch_rejects_more_than_100_skus() {
        List<StockReserveDTO> items = new ArrayList<>(101);
        for (long skuId = 1L; skuId <= 101L; skuId++) {
            items.add(new StockReserveDTO(skuId, 1));
        }

        assertThrows(BizException.class, () -> service.reserveBatch(items));
        verify(manager, never()).deductAvailable(anyLong(), anyLong());
    }


    /**
     * 执行 release_restores_stock 方法。
     */
    public void release_restores_stock() {
        when(manager.restoreReserved(100L, 3L)).thenReturn(1);

        boolean ok = service.release(100L, 3);
        assertEquals(true, ok);
        verify(manager).restoreReserved(100L, 3L);
        verify(journalManager).save(any(StockJournalPO.class));
    }


    /**
     * 执行 release_same_order_is_idempotent 方法。
     */
    public void release_same_order_is_idempotent() {
        when(journalManager.save(any(StockJournalPO.class)))
            .thenThrow(new DuplicateKeyException("duplicate release"));

        boolean ok = service.release(100L, 3, "O100");

        assertTrue(ok);
        verify(manager, never()).restoreReserved(any(), anyLong());
    }


    /**
     * 执行 release_order_insufficient_throws 方法。
     */
    public void release_order_insufficient_throws() {
        when(manager.restoreReserved(100L, 5L)).thenReturn(0);

        assertThrows(BizException.class, () -> service.release(100L, 5, "O100"));
        verify(journalManager).save(any(StockJournalPO.class));
    }


    /**
     * 执行 reserve_missing_throws_biz 方法。
     */
    public void reserve_missing_throws_biz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.reserve(999L, 1));
    }


    /**
     * 执行 release_missing_returns_false 方法。
     */
    public void release_missing_returns_false() {
        when(manager.restoreReserved(999L, 1L)).thenReturn(0);
        boolean ok = service.release(999L, 1);
        assertEquals(false, ok);
    }


    /**
     * 执行 release_insufficient_reserved_returns_false 方法。
     */
    public void release_insufficient_reserved_returns_false() {
        when(manager.restoreReserved(100L, 5L)).thenReturn(0);

        boolean ok = service.release(100L, 5);
        assertEquals(false, ok);
    }


    /**
     * 初始化相关资源。
     */
    public void initStock_creates_record_when_absent() {
        when(manager.getOne(any())).thenReturn(null);

        service.initStock(100L, 10L, 5);

        ArgumentCaptor<StockPO> captor = ArgumentCaptor.forClass(StockPO.class);
        verify(manager).save(captor.capture());
        StockPO saved = captor.getValue();
        assertEquals(100L, saved.getSkuId());
        assertEquals(10L, saved.getMerchantId());
        assertEquals(5L, saved.getAvailable());
        assertEquals(0L, saved.getReserved());
        verify(journalManager).save(any(StockJournalPO.class));
    }


    /**
     * 初始化相关资源。
     */
    public void initStock_skips_when_record_exists() {
        StockPO existing = new StockPO();
        existing.setId(1L);
        existing.setSkuId(100L);
        existing.setMerchantId(10L);
        existing.setAvailable(99L);
        when(manager.getOne(any())).thenReturn(existing);

        service.initStock(100L, 10L, 5);

        verify(manager, never()).save(any(StockPO.class));
        verify(journalManager, never()).save(any(StockJournalPO.class));
    }


    /**
     * 初始化相关资源。
     */
    public void initStock_null_quantity_defaults_zero() {
        when(manager.getOne(any())).thenReturn(null);

        service.initStock(100L, 10L, null);

        ArgumentCaptor<StockPO> captor = ArgumentCaptor.forClass(StockPO.class);
        verify(manager).save(captor.capture());
        assertEquals(0L, captor.getValue().getAvailable());
        verify(journalManager, never()).save(any(StockJournalPO.class));
    }


    /**
     * 初始化相关资源。
     */
    public void initStock_null_sku_throws() {
        assertThrows(BizException.class, () -> service.initStock(null, 10L, 5));
    }


    /**
     * 执行 query_returns_available 方法。
     */
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
     * 执行 query_missing_returns_zero 方法。
     */
    public void query_missing_returns_zero() {
        when(manager.getOne(any())).thenReturn(null);
        long available = service.query(999L);
        assertEquals(0L, available);
    }


    /**
     * 执行 query_merchant_owned_returns_available 方法。
     */
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
     * 执行 query_merchant_mismatch_throws 方法。
     */
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
     * 分页查询数据。
     */
    public void page_delegates_to_manager() {
        StockPageDTO dto = new StockPageDTO();
        List<StockPO> mockRecords = new ArrayList<>();
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(mockRecords);

        CursorPageVO<StockPO> result = service.page(dto);
        assertNotNull(result);
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 执行 platformStatistics_computes_alert_ratio 方法。
     */
    public void platformStatistics_computes_alert_ratio() {
        StockStatisticsDTO stat = new StockStatisticsDTO(10L, 1000L, 200L, 3L);
        when(manager.statistics()).thenReturn(stat);

        StockStatisticsVO result = service.platformStatistics();
        assertEquals(10L, result.getTotalSkuCount());
        assertEquals(1000L, result.getTotalAvailable());
        assertEquals(200L, result.getTotalReserved());
        assertEquals(3L, result.getAlertSkuCount());
        assertEquals(new BigDecimal("0.30"), result.getAlertRatio());
    }


    /**
     * 执行 platformStatistics_null_returns_zero 方法。
     */
    public void platformStatistics_null_returns_zero() {
        when(manager.statistics()).thenReturn(null);

        StockStatisticsVO result = service.platformStatistics();
        assertEquals(0L, result.getTotalSkuCount());
        assertEquals(BigDecimal.ZERO, result.getAlertRatio());
    }


    /**
     * 执行 exportJournal_delegates_to_journalManager 方法。
     */
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
     * 执行 exportJournal_merchant_mismatch_throws 方法。
     */
    public void exportJournal_merchant_mismatch_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.exportJournal(100L, 2L));
        verify(journalManager, never()).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 执行 transfer_success_decrements_and_increments 方法。
     */
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
        when(manager.deductAvailable(100L, 3L)).thenReturn(1);
        when(manager.increaseAvailable(200L, 3L)).thenReturn(1);

        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromSkuId(100L);
        dto.setToSkuId(200L);
        dto.setQuantity(3L);
        dto.setReason("test");
        dto.setOperatorId(1L);
        service.transfer(dto);

        assertEquals(7L, from.getAvailable());
        assertEquals(8L, to.getAvailable());
        verify(manager).deductAvailable(100L, 3L);
        verify(manager).increaseAvailable(200L, 3L);
        verify(transferManager).save(any(StockTransferPO.class));
    }


    /**
     * 执行 transfer_same_sku_throws 方法。
     */
    public void transfer_same_sku_throws() {
        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromSkuId(100L);
        dto.setToSkuId(100L);
        dto.setQuantity(1L);
        assertThrows(BizException.class, () -> service.transfer(dto));
    }


    /**
     * 执行 transfer_insufficient_throws 方法。
     */
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
     * 执行 transfer_target_not_found_throws 方法。
     */
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
     * 执行 transferPage_delegates_to_manager 方法。
     */
    public void transferPage_delegates_to_manager() {
        StockTransferPageDTO dto = new StockTransferPageDTO();
        List<StockTransferPO> mockRecords = new ArrayList<>();
        when(transferManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(mockRecords);

        CursorPageVO<StockTransferPO> result = service.transferPage(dto);
        assertNotNull(result);
        verify(transferManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }


    /**
     * 创建业务数据。
     */
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
     * 执行 completeCountTask_with_diff_adjusts_stock 方法。
     */
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
     * 执行 completeCountTask_no_diff_succeeds 方法。
     */
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
     * 执行 completeCountTask_concurrent_update_miss_throws 方法。
     */
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
     * 执行 adjustStock_concurrent_update_throws 方法。
     */
    public void adjustStock_concurrent_update_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(manager.updateById(any(StockPO.class))).thenReturn(false);

        assertThrows(BizException.class, () -> service.adjustStock(100L, 5L, "replenish"));
    }


    /**
     * 执行 completeCountTask_not_pending_throws 方法。
     */
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
     * 执行 cancelCountTask_succeeds 方法。
     */
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
     * 执行 cancelCountTask_not_pending_throws 方法。
     */
    public void cancelCountTask_not_pending_throws() {
        StockCountTaskPO task = new StockCountTaskPO();
        task.setId(1L);
        task.setStatus(StockCountTaskStatusEnum.CANCELLED.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);

        assertThrows(BizException.class, () -> service.cancelCountTask(1L, 99L));
    }


    /**
     * 统计业务数据。
     */
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
     * 创建业务数据。
     */
    public void createCountTask_stock_not_found_throws() {
        when(manager.getOne(any())).thenReturn(null);

        StockCountTaskCreateDTO dto = new StockCountTaskCreateDTO();
        dto.setSkuId(999L);
        dto.setOperatorId(1L);
        assertThrows(BizException.class, () -> service.createCountTask(dto));
    }


    /**
     * 查询列表数据。
     */
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
     * 执行 correctStock_succeeds 方法。
     */
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
     * 执行 correctStock_negative_quantity_throws 方法。
     */
    public void correctStock_negative_quantity_throws() {
        assertThrows(BizException.class, () -> service.correctStock(100L, -1L, "test"));
    }


    /**
     * 执行 correctStock_stock_not_found_throws 方法。
     */
    public void correctStock_stock_not_found_throws() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.correctStock(999L, 10L, "test"));
    }


    /**
     * 执行 correctStock_concurrent_update_throws 方法。
     */
    public void correctStock_concurrent_update_throws() {
        StockPO s = new StockPO();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(manager.updateById(any(StockPO.class))).thenReturn(false);

        assertThrows(BizException.class, () -> service.correctStock(100L, 20L, "test"));
    }


    /**
     * 执行 adjustStock_positive_increments_available 方法。
     */
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
     * 执行 adjustStock_negative_decrements_available 方法。
     */
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
     * 执行 adjustStock_negative_below_zero_throws 方法。
     */
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
     * 执行 adjustStock_null_quantity_throws 方法。
     */
    public void adjustStock_null_quantity_throws() {
        assertThrows(BizException.class, () -> service.adjustStock(100L, null, "test"));
    }


    /**
     * 执行 adjustStock_noRecord_createsThenIncrements 方法。
     */
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
     * 执行 adjustStock_merchant_mismatch_throws 方法。
     */
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
     * 执行 adjustStock_merchant_null_ownership_throws 方法。
     */
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
     * 执行 adjustStock_merchant_noRecord_creates_with_merchantId 方法。
     */
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
     * 设置相关数据。
     */
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
     * 设置相关数据。
     */
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
     * 设置相关数据。
     */
    public void setThreshold_null_throws() {
        assertThrows(BizException.class, () -> service.setThreshold(100L, null));
    }


    /**
     * 设置相关数据。
     */
    public void setThreshold_negative_throws() {
        assertThrows(BizException.class, () -> service.setThreshold(100L, -1L));
    }


    /**
     * 设置相关数据。
     */
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
     * 执行 queryJournal_delegates_to_journalManager 方法。
     */
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
     * 执行 queryJournal_applies_limit_200 方法。
     */
    public void queryJournal_applies_limit_200() {
        when(journalManager.list(any(Wrapper.class))).thenReturn(new ArrayList<>());

        service.queryJournal(100L);

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(journalManager).list(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().contains("LIMIT 200"));
    }


    /**
     * 执行 queryJournal_merchant_mismatch_throws 方法。
     */
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
     * 查询列表数据。
     */
    public void listAbnormalStock_applies_limit_200() {
        when(manager.list(any(Wrapper.class))).thenReturn(new ArrayList<>());

        service.listAbnormalStock();

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(manager).list(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().contains("LIMIT 200"));
    }


    /**
     * 执行 completeCountTask_not_found_throws 方法。
     */
    public void completeCountTask_not_found_throws() {
        when(countTaskManager.getById(999L)).thenReturn(null);

        StockCountTaskCompleteDTO dto = new StockCountTaskCompleteDTO();
        dto.setActualQuantity(10L);
        assertThrows(BizException.class, () -> service.completeCountTask(999L, dto));
    }


    /**
     * 执行 cancelCountTask_not_found_throws 方法。
     */
    public void cancelCountTask_not_found_throws() {
        when(countTaskManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.cancelCountTask(999L, 99L));
    }
}
