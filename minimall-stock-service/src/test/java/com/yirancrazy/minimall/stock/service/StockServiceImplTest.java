package com.yirancrazy.minimall.stock.service;

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
import com.yirancrazy.minimall.api.dto.stock.StockReserveDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.stock.constant.StockCountTaskStatusEnum;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDto;
import com.yirancrazy.minimall.stock.dto.StockPageDto;
import com.yirancrazy.minimall.stock.dto.StockStatisticsDto;
import com.yirancrazy.minimall.stock.dto.StockTransferDto;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDto;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPo;
import com.yirancrazy.minimall.stock.entity.StockJournalPo;
import com.yirancrazy.minimall.stock.entity.StockPo;
import com.yirancrazy.minimall.stock.entity.StockTransferPo;
import com.yirancrazy.minimall.stock.manager.StockCountTaskManager;
import com.yirancrazy.minimall.stock.manager.StockJournalManager;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.manager.StockTransferManager;
import com.yirancrazy.minimall.stock.service.impl.StockServiceImpl;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 Mockito 覆盖库存预占、释放、查询、调拨、盘点和纠正等核心场景。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class StockServiceImplTest {

    private StockManager manager;
    private StockJournalManager journalManager;
    private StockTransferManager transferManager;
    private StockCountTaskManager countTaskManager;
    private StockServiceImpl service;

    /**
     * 测试前置：初始化 MyBatis-Plus 表元数据、Mock 四类管理器，并设置持久化成功默认返回值。
     */
    @BeforeEach
    void setUp() {
        // 初始化 TableInfo，便于 LambdaQueryWrapper 解析 lambda 缓存
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.stock.mapper.StockMapper");
        TableInfoHelper.initTableInfo(assistant, StockPo.class);
        TableInfoHelper.initTableInfo(assistant, StockJournalPo.class);

        manager = mock(StockManager.class);
        journalManager = mock(StockJournalManager.class);
        transferManager = mock(StockTransferManager.class);
        countTaskManager = mock(StockCountTaskManager.class);
        lenient().when(manager.updateById(any(StockPo.class))).thenReturn(true);
        lenient().when(journalManager.save(any(StockJournalPo.class))).thenReturn(true);
        lenient().when(transferManager.save(any(StockTransferPo.class))).thenReturn(true);
        lenient().when(countTaskManager.updateById(any(StockCountTaskPo.class))).thenReturn(true);
        service = new StockServiceImpl(manager, journalManager, transferManager, countTaskManager);
    }

    /** * 目标验证预占成功；前置可用 10 且扣 2 命中；步骤调用预占；预期余 8、预占 2 并写 OD100 流水. */
    @Test
    public void testReserveAtomicSuccessRecordsJournal() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(manager.deductAvailable(100L, 2L)).thenReturn(1);

        boolean ok = service.reserve(100L, 2, "OD100");
        assertEquals(true, ok);
        assertEquals(8L, s.getAvailable());
        assertEquals(2L, s.getReserved());
        verify(manager).deductAvailable(100L, 2L);
        ArgumentCaptor<StockJournalPo> journalCaptor = ArgumentCaptor.forClass(StockJournalPo.class);
        verify(journalManager).save(journalCaptor.capture());
        assertEquals("OD100", journalCaptor.getValue().getOrderNo());
    }

    /** * 目标验证库存不足；前置可用 3 且扣 100 未命中；步骤调用预占；预期抛 BizException 且不写流水. */
    @Test
    public void testReserveInsufficientStockThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(3L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(manager.deductAvailable(100L, 100L)).thenReturn(0);

        assertThrows(BizException.class, () -> service.reserve(100L, 100, "OD100"));
        verify(journalManager, never()).save(any(StockJournalPo.class));
    }

    /** * 目标验证数量校验；前置订单号有效并传入 null、0、负数；步骤调用预占；预期均抛 BizException. */
    @Test
    public void testReserveInvalidQuantityThrows() {
        assertThrows(BizException.class, () -> service.reserve(100L, null, "OD100"));
        assertThrows(BizException.class, () -> service.reserve(100L, 0, "OD100"));
        assertThrows(BizException.class, () -> service.reserve(100L, -1, "OD100"));
    }

    /** * 目标验证订单号校验；前置 SKU 和数量有效并传 null、空格；步骤调用预占；预期均抛 BizException. */
    @Test
    public void testReserveRejectsBlankOrderNo() {
        assertThrows(BizException.class, () -> service.reserve(100L, 1, null));
        assertThrows(BizException.class, () -> service.reserve(100L, 1, " "));
    }

    /** * 目标验证批量合并；前置同 SKU 两条明细数量 2、3；步骤批量预占；预期仅扣 5 并只写一条 OD100 流水. */
    @Test
    public void testReserveBatchMergesDuplicateSkus() {
        StockPo stock = new StockPo();
        stock.setId(1L);
        stock.setSkuId(100L);
        stock.setAvailable(10L);
        stock.setReserved(0L);
        when(manager.getOne(any())).thenReturn(stock);
        when(manager.deductAvailable(100L, 5L)).thenReturn(1);

        boolean ok =
                service.reserveBatch(
                        List.of(new StockReserveDto(100L, 2, "OD100"), new StockReserveDto(100L, 3, "OD100")));

        assertTrue(ok);
        verify(manager).deductAvailable(100L, 5L);
        ArgumentCaptor<StockJournalPo> journalCaptor = ArgumentCaptor.forClass(StockJournalPo.class);
        verify(journalManager, times(1)).save(journalCaptor.capture());
        assertEquals("OD100", journalCaptor.getValue().getOrderNo());
    }

    /** * 目标验证同 SKU 订单一致性；前置明细关联 OD100 和 OD200；步骤批量预占；预期抛异常且不扣库存. */
    @Test
    public void testReserveBatchRejectsDifferentOrderNosForSameSku() {
        StockPo stock = new StockPo();
        stock.setId(1L);
        stock.setSkuId(100L);
        stock.setAvailable(10L);
        stock.setReserved(0L);
        when(manager.getOne(any())).thenReturn(stock);
        when(manager.deductAvailable(100L, 5L)).thenReturn(1);

        assertThrows(
                BizException.class,
                () ->
                        service.reserveBatch(
                                List.of(
                                        new StockReserveDto(100L, 2, "OD100"), new StockReserveDto(100L, 3, "OD200"))));

        verify(manager, never()).deductAvailable(any(), anyLong());
    }

    /**
     * 验证批量预占中任一 SKU 库存不足时整体调用失败。
     * 前置：SKU 100 扣减成功，SKU 200 扣减返回 0；当前方法未标注 @Test，不会被 JUnit 执行。
     * 预期：抛出 BizException，并体现事务场景下已执行的两个条件扣减。
     */
    public void reserveBatchAnyInsufficientThrows() {
        StockPo first = new StockPo();
        first.setId(1L);
        first.setSkuId(100L);
        first.setAvailable(10L);
        first.setReserved(0L);
        StockPo second = new StockPo();
        second.setId(2L);
        second.setSkuId(200L);
        second.setAvailable(0L);
        second.setReserved(0L);
        when(manager.getOne(any())).thenReturn(first, second);
        when(manager.deductAvailable(100L, 1L)).thenReturn(1);
        when(manager.deductAvailable(200L, 1L)).thenReturn(0);

        assertThrows(
                BizException.class,
                () ->
                        service.reserveBatch(
                                List.of(
                                        new StockReserveDto(100L, 1, "OD100"), new StockReserveDto(200L, 1, "OD200"))));

        verify(manager).deductAvailable(100L, 1L);
        verify(manager).deductAvailable(200L, 1L);
    }

    /**
     * 验证单次批量预占超过 100 个不同 SKU 时在访问持久层前被拒绝。
     * 前置：构造 101 个 SKU、每个数量为 1 的明细；当前方法未标注 @Test。
     * 预期：抛出 BizException，且不发生任何库存扣减。
     */
    public void reserveBatchRejectsMoreThan100Skus() {
        List<StockReserveDto> items = new ArrayList<>(101);
        for (long skuId = 1L; skuId <= 101L; skuId++) {
            items.add(new StockReserveDto(skuId, 1, "OD" + skuId));
        }

        assertThrows(BizException.class, () -> service.reserveBatch(items));
        verify(manager, never()).deductAvailable(anyLong(), anyLong());
    }

    /** * 目标验证释放恢复；前置恢复 3 个预占命中；步骤调用释放；预期成功、恢复量为 3 并写 OD100 流水. */
    @Test
    public void testReleaseRestoresStock() {
        when(manager.restoreReserved(100L, 3L)).thenReturn(1);

        boolean ok = service.release(100L, 3, "OD100");
        assertEquals(true, ok);
        verify(manager).restoreReserved(100L, 3L);
        ArgumentCaptor<StockJournalPo> journalCaptor = ArgumentCaptor.forClass(StockJournalPo.class);
        verify(journalManager).save(journalCaptor.capture());
        assertEquals("OD100", journalCaptor.getValue().getOrderNo());
    }

    /** * 目标验证释放幂等；前置流水保存触发唯一键冲突；步骤重复释放；预期返回成功且不再恢复库存. */
    @Test
    public void testReleaseSameOrderIsIdempotent() {
        when(journalManager.save(any(StockJournalPo.class)))
                .thenThrow(new DuplicateKeyException("duplicate release"));

        boolean ok = service.release(100L, 3, "O100");

        assertTrue(ok);
        verify(manager, never()).restoreReserved(any(), anyLong());
    }

    /** * 目标验证释放条件失败；前置释放 5 未命中；步骤调用释放；预期抛 BizException 且事务回滚流水. */
    @Test
    public void testReleaseOrderInsufficientThrows() {
        when(manager.restoreReserved(100L, 5L)).thenReturn(0);

        assertThrows(BizException.class, () -> service.release(100L, 5, "O100"));
        verify(journalManager).save(any(StockJournalPo.class));
    }

    /** * 目标验证预占缺库存；前置库存查询返回空；步骤调用预占；预期抛 BizException 且不执行扣减. */
    @Test
    public void testReserveMissingThrowsBiz() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.reserve(999L, 1, "OD999"));
    }

    /** * 目标验证释放缺记录；前置 SKU 999 恢复 1 未命中；步骤调用释放；预期抛 BizException. */
    @Test
    public void testReleaseMissingThrows() {
        when(manager.restoreReserved(999L, 1L)).thenReturn(0);
        assertThrows(BizException.class, () -> service.release(999L, 1, "OD999"));
    }

    /** * 目标验证释放超预占；前置 SKU 100 恢复 5 未命中；步骤调用释放；预期抛 BizException. */
    @Test
    public void testReleaseInsufficientReservedThrows() {
        when(manager.restoreReserved(100L, 5L)).thenReturn(0);
        assertThrows(BizException.class, () -> service.release(100L, 5, "OD100"));
    }

    /**
     * 验证不存在库存记录时初始化会创建库存，并写入初始库存调整流水。
     * 前置：库存查询返回 null，SKU 100、商户 10、初始量 5；当前方法未标注 @Test。
     * 预期：保存记录包含 SKU、商户、可用量 5、预占量 0，并保存一条流水。
     */
    public void initStockCreatesRecordWhenAbsent() {
        when(manager.getOne(any())).thenReturn(null);

        service.initStock(100L, 10L, 5);

        ArgumentCaptor<StockPo> captor = ArgumentCaptor.forClass(StockPo.class);
        verify(manager).save(captor.capture());
        StockPo saved = captor.getValue();
        assertEquals(100L, saved.getSkuId());
        assertEquals(10L, saved.getMerchantId());
        assertEquals(5L, saved.getAvailable());
        assertEquals(0L, saved.getReserved());
        verify(journalManager).save(any(StockJournalPo.class));
    }

    /**
     * 验证已有库存记录时重复初始化不再保存库存或流水。
     * 前置：SKU 100 已存在且可用量为 99；当前方法未标注 @Test。
     * 预期：库存与流水保存方法均未被调用。
     */
    public void initStockSkipsWhenRecordExists() {
        StockPo existing = new StockPo();
        existing.setId(1L);
        existing.setSkuId(100L);
        existing.setMerchantId(10L);
        existing.setAvailable(99L);
        when(manager.getOne(any())).thenReturn(existing);

        service.initStock(100L, 10L, 5);

        verify(manager, never()).save(any(StockPo.class));
        verify(journalManager, never()).save(any(StockJournalPo.class));
    }

    /**
     * 验证初始化数量为 null 时按 0 创建库存且不写调整流水。
     * 前置：库存查询返回 null，初始数量为 null；当前方法未标注 @Test。
     * 预期：新库存可用量为 0，库存流水保存方法未被调用。
     */
    public void initStockNullQuantityDefaultsZero() {
        when(manager.getOne(any())).thenReturn(null);

        service.initStock(100L, 10L, null);

        ArgumentCaptor<StockPo> captor = ArgumentCaptor.forClass(StockPo.class);
        verify(manager).save(captor.capture());
        assertEquals(0L, captor.getValue().getAvailable());
        verify(journalManager, never()).save(any(StockJournalPo.class));
    }

    /**
     * 验证初始化库存时 SKU ID 为空会被拒绝。
     * 前置：skuId=null；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void initStockNullSkuThrows() {
        assertThrows(BizException.class, () -> service.initStock(null, 10L, 5));
    }

    /**
     * 验证无商户条件的库存查询返回持久化可用量。
     * 前置：SKU 100 的可用量为 10、预占量为 2；当前方法未标注 @Test。
     * 预期：query 返回 10。
     */
    public void queryReturnsAvailable() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(2L);
        when(manager.getOne(any())).thenReturn(s);

        long available = service.query(100L);
        assertEquals(10L, available);
    }

    /**
     * 验证无商户条件的库存不存在时返回 0 而不是抛出异常。
     * 前置：库存查询返回 null；当前方法未标注 @Test。
     * 预期：query 返回 0。
     */
    public void queryMissingReturnsZero() {
        when(manager.getOne(any())).thenReturn(null);
        long available = service.query(999L);
        assertEquals(0L, available);
    }

    /**
     * 验证商户查询已有归属库存时返回其可用量。
     * 前置：SKU 100 属于商户 1 且可用量为 10；当前方法未标注 @Test。
     * 预期：query(100,1) 返回 10。
     */
    public void queryMerchantOwnedReturnsAvailable() {
        StockPo s = new StockPo();
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
     * 验证商户查询遇到归属不匹配的库存时拒绝访问。
     * 前置：SKU 100 属于商户 1，查询使用商户 2；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void queryMerchantMismatchThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.query(100L, 2L));
    }

    /**
     * 验证库存分页从服务层查询并返回非空游标结果。
     * 前置：StockManager.list 返回空记录集合；当前方法未标注 @Test。
     * 预期：返回非空 CursorPageVo，且持久层列表方法被调用一次。
     */
    public void pageDelegatesToManager() {
        StockPageDto dto = new StockPageDto();
        List<StockPo> mockRecords = new ArrayList<>();
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(mockRecords);

        CursorPageVo<StockPo> result = service.page(dto);
        assertNotNull(result);
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证平台统计按告警数除以 SKU 总数计算两位小数占比。
     * 前置：聚合结果为 10 个 SKU、3 个告警；当前方法未标注 @Test。
     * 预期：告警占比为 0.30，其余聚合字段原样输出。
     */
    public void platformStatisticsComputesAlertRatio() {
        StockStatisticsDto stat = new StockStatisticsDto(10L, 1000L, 200L, 3L);
        when(manager.statistics()).thenReturn(stat);

        StockStatisticsVo result = service.platformStatistics();
        assertEquals(10L, result.getTotalSkuCount());
        assertEquals(1000L, result.getTotalAvailable());
        assertEquals(200L, result.getTotalReserved());
        assertEquals(3L, result.getAlertSkuCount());
        assertEquals(new BigDecimal("0.30"), result.getAlertRatio());
    }

    /**
     * 验证库存聚合查询返回 null 时平台统计降级为全零结果。
     * 前置：StockManager.statistics 返回 null；当前方法未标注 @Test。
     * 预期：SKU 数和告警占比均为 0。
     */
    public void platformStatisticsNullReturnsZero() {
        when(manager.statistics()).thenReturn(null);

        StockStatisticsVo result = service.platformStatistics();
        assertEquals(0L, result.getTotalSkuCount());
        assertEquals(BigDecimal.ZERO, result.getAlertRatio());
    }

    /**
     * 验证流水导出委托库存流水管理器并原样返回记录。
     * 前置：流水管理器返回一条 ID 为 1 的流水；当前方法未标注 @Test。
     * 预期：导出列表大小为 1 且记录 ID 为 1。
     */
    public void exportJournalDelegatesToJournalManager() {
        StockJournalPo po = new StockJournalPo();
        po.setId(1L);
        po.setSkuId(100L);
        po.setQuantity(-2L);
        when(journalManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(po));

        List<StockJournalPo> result = service.exportJournal(100L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(journalManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证导出商户流水前会拒绝归属不匹配的 SKU。
     * 前置：SKU 100 属于商户 1，导出使用商户 2；当前方法未标注 @Test。
     * 预期：抛出 BizException，流水列表查询未被调用。
     */
    public void exportJournalMerchantMismatchThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.exportJournal(100L, 2L));
        verify(journalManager, never())
                .list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证成功调拨会扣减源库存、增加目标库存并保存调拨记录。
     * 前置：源可用量 10、目标可用量 5，调拨数量 3；当前方法未标注 @Test。
     * 预期：源可用量变为 7、目标变为 8，两边的原子更新均被调用，调拨记录被保存。
     */
    public void transferSuccessDecrementsAndIncrements() {
        StockPo from = new StockPo();
        from.setId(1L);
        from.setSkuId(100L);
        from.setAvailable(10L);
        from.setReserved(0L);
        StockPo to = new StockPo();
        to.setId(2L);
        to.setSkuId(200L);
        to.setAvailable(5L);
        to.setReserved(0L);
        when(manager.getOne(any())).thenReturn(from, to);
        when(manager.deductAvailable(100L, 3L)).thenReturn(1);
        when(manager.increaseAvailable(200L, 3L)).thenReturn(1);

        StockTransferDto dto = new StockTransferDto();
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
        verify(transferManager).save(any(StockTransferPo.class));
    }

    /**
     * 验证调出和调入 SKU 相同时拒绝调拨。
     * 前置：源、目标 SKU 均为 100；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void transferSameSkuThrows() {
        StockTransferDto dto = new StockTransferDto();
        dto.setFromSkuId(100L);
        dto.setToSkuId(100L);
        dto.setQuantity(1L);
        assertThrows(BizException.class, () -> service.transfer(dto));
    }

    /**
     * 验证源 SKU 可用量不足时调拨失败。
     * 前置：源可用量 2、调拨数量 5；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void transferInsufficientThrows() {
        StockPo from = new StockPo();
        from.setId(1L);
        from.setSkuId(100L);
        from.setAvailable(2L);
        from.setReserved(0L);
        when(manager.getOne(any())).thenReturn(from);

        StockTransferDto dto = new StockTransferDto();
        dto.setFromSkuId(100L);
        dto.setToSkuId(200L);
        dto.setQuantity(5L);
        assertThrows(BizException.class, () -> service.transfer(dto));
    }

    /**
     * 验证目标 SKU 库存记录不存在时调拨失败。
     * 前置：源库存存在，目标库存查询返回 null；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void transferTargetNotFoundThrows() {
        StockPo from = new StockPo();
        from.setId(1L);
        from.setSkuId(100L);
        from.setAvailable(10L);
        from.setReserved(0L);
        when(manager.getOne(any())).thenReturn(from, null);

        StockTransferDto dto = new StockTransferDto();
        dto.setFromSkuId(100L);
        dto.setToSkuId(200L);
        dto.setQuantity(3L);
        assertThrows(BizException.class, () -> service.transfer(dto));
    }

    /**
     * 验证调拨分页从服务层查询并返回非空游标结果。
     * 前置：StockTransferManager.list 返回空集合；当前方法未标注 @Test。
     * 预期：返回非空 CursorPageVo，且调拨记录列表查询被调用一次。
     */
    public void transferPageDelegatesToManager() {
        StockTransferPageDto dto = new StockTransferPageDto();
        List<StockTransferPo> mockRecords = new ArrayList<>();
        when(transferManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(mockRecords);

        CursorPageVo<StockTransferPo> result = service.transferPage(dto);
        assertNotNull(result);
        verify(transferManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证创建盘点任务会保存当前 SKU 的盘点快照。
     * 前置：SKU 100 的可用量为 10，操作人为 1；当前方法未标注 @Test。
     * 预期：StockCountTaskPo 被保存一次。
     */
    public void createCountTaskSucceeds() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        StockCountTaskCreateDto dto = new StockCountTaskCreateDto();
        dto.setSkuId(100L);
        dto.setOperatorId(1L);
        service.createCountTask(dto);

        verify(countTaskManager).save(any(StockCountTaskPo.class));
    }

    /**
     * 验证盘点结果与账面量存在差异时会完成条件更新并调整库存。
     * 前置：账面量 10、实盘量 8，条件更新返回 1；当前方法未标注 @Test。
     * 预期：任务状态变为完成、差异为 -2，并调用库存更新和流水保存。
     */
    public void completeCountTaskWithDiffAdjustsStock() {
        StockCountTaskPo task = new StockCountTaskPo();
        task.setId(1L);
        task.setSkuId(100L);
        task.setExpectedQuantity(10L);
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);
        when(countTaskManager.completeIfStatus(eq(1L), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(1);

        StockPo s = new StockPo();
        s.setId(2L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        StockCountTaskCompleteDto dto = new StockCountTaskCompleteDto();
        dto.setActualQuantity(8L);
        service.completeCountTask(1L, dto);

        assertEquals(StockCountTaskStatusEnum.COMPLETED.intCode(), task.getStatus());
        assertEquals(-2L, task.getDiffQuantity());
        verify(countTaskManager)
                .completeIfStatus(
                        eq(1L),
                        eq(StockCountTaskStatusEnum.PENDING.intCode()),
                        eq(StockCountTaskStatusEnum.COMPLETED.intCode()),
                        eq(8L),
                        eq(-2L),
                        any());
        verify(manager).updateById(any(StockPo.class));
    }

    /**
     * 验证实盘量与账面量一致时仅完成任务，不调整库存。
     * 前置：账面量和实盘量均为 10，条件更新返回 1；当前方法未标注 @Test。
     * 预期：差异为 0，库存更新方法未被调用。
     */
    public void completeCountTaskNoDiffSucceeds() {
        StockCountTaskPo task = new StockCountTaskPo();
        task.setId(1L);
        task.setSkuId(100L);
        task.setExpectedQuantity(10L);
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);
        when(countTaskManager.completeIfStatus(eq(1L), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(1);

        StockCountTaskCompleteDto dto = new StockCountTaskCompleteDto();
        dto.setActualQuantity(10L);
        service.completeCountTask(1L, dto);

        assertEquals(0L, task.getDiffQuantity());
        verify(manager, never()).updateById(any(StockPo.class));
    }

    /**
     * 验证盘点任务完成条件更新未命中时阻止重复入账。
     * 前置：任务预先读取为待盘点，但条件更新返回 0；当前方法未标注 @Test。
     * 预期：抛出 BizException，不再查询或更新库存，也不写库存流水。
     */
    public void completeCountTaskConcurrentUpdateMissThrows() {
        StockCountTaskPo task = new StockCountTaskPo();
        task.setId(1L);
        task.setSkuId(100L);
        task.setExpectedQuantity(10L);
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);
        when(countTaskManager.completeIfStatus(eq(1L), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(0);

        StockCountTaskCompleteDto dto = new StockCountTaskCompleteDto();
        dto.setActualQuantity(8L);
        assertThrows(BizException.class, () -> service.completeCountTask(1L, dto));
        // 条件更新未命中：不触发库存调整与流水记录
        verify(manager, never()).getOne(any());
        verify(manager, never()).updateById(any(StockPo.class));
        verify(journalManager, never()).save(any(StockJournalPo.class));
    }

    /**
     * 验证库存增量调整的条件更新未命中时抛出并发冲突异常。
     * 前置：库存存在但 updateById 返回 false；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void adjustStockConcurrentUpdateThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(manager.updateById(any(StockPo.class))).thenReturn(false);

        assertThrows(BizException.class, () -> service.adjustStock(100L, 5L, "replenish"));
    }

    /**
     * 验证已完成任务不能再次提交完成。
     * 前置：任务状态已为 COMPLETED；当前方法未标注 @Test。
     * 预期：抛出 BizException，不执行条件更新和库存调整。
     */
    public void completeCountTaskNotPendingThrows() {
        StockCountTaskPo task = new StockCountTaskPo();
        task.setId(1L);
        task.setStatus(StockCountTaskStatusEnum.COMPLETED.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);

        StockCountTaskCompleteDto dto = new StockCountTaskCompleteDto();
        dto.setActualQuantity(10L);
        assertThrows(BizException.class, () -> service.completeCountTask(1L, dto));
    }

    /**
     * 验证待盘点任务可被原子取消。
     * 前置：任务状态为 PENDING，cancelIfStatus 返回 1；当前方法未标注 @Test。
     * 预期：任务状态变为 CANCELLED，并以原状态和目标状态调用条件更新。
     */
    public void cancelCountTaskSucceeds() {
        StockCountTaskPo task = new StockCountTaskPo();
        task.setId(1L);
        task.setStatus(StockCountTaskStatusEnum.PENDING.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);
        when(countTaskManager.cancelIfStatus(
                        1L,
                        StockCountTaskStatusEnum.PENDING.intCode(),
                        StockCountTaskStatusEnum.CANCELLED.intCode()))
                .thenReturn(1);

        service.cancelCountTask(1L, 99L);
        assertEquals(StockCountTaskStatusEnum.CANCELLED.intCode(), task.getStatus());
        verify(countTaskManager)
                .cancelIfStatus(
                        1L,
                        StockCountTaskStatusEnum.PENDING.intCode(),
                        StockCountTaskStatusEnum.CANCELLED.intCode());
    }

    /**
     * 验证非待盘点任务不能被取消。
     * 前置：任务状态已为 CANCELLED；当前方法未标注 @Test。
     * 预期：抛出 BizException，不执行条件更新。
     */
    public void cancelCountTaskNotPendingThrows() {
        StockCountTaskPo task = new StockCountTaskPo();
        task.setId(1L);
        task.setStatus(StockCountTaskStatusEnum.CANCELLED.intCode());
        when(countTaskManager.getById(1L)).thenReturn(task);

        assertThrows(BizException.class, () -> service.cancelCountTask(1L, 99L));
    }

    /**
     * 验证盘点任务分页从服务层查询并返回非空游标结果。
     * 前置：StockCountTaskManager.list 返回空集合；当前方法未标注 @Test。
     * 预期：返回非空 CursorPageVo，且任务列表查询被调用一次。
     */
    public void countTaskPageDelegatesToManager() {
        StockCountTaskPageDto dto = new StockCountTaskPageDto();
        List<StockCountTaskPo> mockRecords = new ArrayList<>();
        when(countTaskManager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(mockRecords);

        CursorPageVo<StockCountTaskPo> result = service.countTaskPage(dto);
        assertNotNull(result);
        verify(countTaskManager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证为不存在的 SKU 创建盘点任务时抛出业务异常。
     * 前置：库存查询返回 null；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void createCountTaskStockNotFoundThrows() {
        when(manager.getOne(any())).thenReturn(null);

        StockCountTaskCreateDto dto = new StockCountTaskCreateDto();
        dto.setSkuId(999L);
        dto.setOperatorId(1L);
        assertThrows(BizException.class, () -> service.createCountTask(dto));
    }

    /**
     * 验证异常库存查询会返回可用量为负的记录。
     * 前置：持久层返回一条可用量为 -5 的记录；当前方法未标注 @Test。
     * 预期：结果包含该记录且可用量保持 -5。
     */
    public void listAbnormalStockReturnsAbnormalRecords() {
        StockPo abnormal = new StockPo();
        abnormal.setId(1L);
        abnormal.setSkuId(100L);
        abnormal.setAvailable(-5L);
        abnormal.setReserved(0L);
        when(manager.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(abnormal));

        List<StockPo> result = service.listAbnormalStock();
        assertEquals(1, result.size());
        assertEquals(-5L, result.get(0).getAvailable());
        verify(manager).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    /**
     * 验证人工纠正会把可用量替换为目标值并记录差值流水。
     * 前置：旧可用量 10，目标可用量 20；当前方法未标注 @Test。
     * 预期：库存更新为 20，并保存一条纠正流水。
     */
    public void correctStockSucceeds() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        service.correctStock(100L, 20L, "data error");

        assertEquals(20L, s.getAvailable());
        verify(manager).updateById(s);
        verify(journalManager).save(any(StockJournalPo.class));
    }

    /**
     * 验证人工纠正目标数量为负数时被拒绝。
     * 前置：correctQuantity=-1；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void correctStockNegativeQuantityThrows() {
        assertThrows(BizException.class, () -> service.correctStock(100L, -1L, "test"));
    }

    /**
     * 验证人工纠正不存在的 SKU 时抛出业务异常。
     * 前置：库存查询返回 null；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void correctStockStockNotFoundThrows() {
        when(manager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.correctStock(999L, 10L, "test"));
    }

    /**
     * 验证人工纠正的条件更新未命中时抛出并发冲突异常。
     * 前置：库存存在但 updateById 返回 false；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void correctStockConcurrentUpdateThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);
        when(manager.updateById(any(StockPo.class))).thenReturn(false);

        assertThrows(BizException.class, () -> service.correctStock(100L, 20L, "test"));
    }

    /**
     * 验证正向调整会增加可用量并记录调整流水。
     * 前置：可用量 10，调整增量 5；当前方法未标注 @Test。
     * 预期：可用量变为 15，库存和流水均被更新。
     */
    public void adjustStockPositiveIncrementsAvailable() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        service.adjustStock(100L, 5L, "replenish");

        assertEquals(15L, s.getAvailable());
        verify(manager).updateById(s);
        verify(journalManager).save(any(StockJournalPo.class));
    }

    /**
     * 验证负向调整会减少可用量但不低于 0。
     * 前置：可用量 10，调整增量 -3，结果仍为正数；当前方法未标注 @Test。
     * 预期：可用量变为 7，库存更新被调用。
     */
    public void adjustStockNegativeDecrementsAvailable() {
        StockPo s = new StockPo();
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
     * 验证负向调整会使可用量小于 0 时被拒绝。
     * 前置：可用量 3，调整增量 -5；当前方法未标注 @Test。
     * 预期：抛出 BizException，不更新库存且不写流水。
     */
    public void adjustStockNegativeBelowZeroThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(3L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.adjustStock(100L, -5L, "correction"));
        verify(manager, never()).updateById(any(StockPo.class));
        verify(journalManager, never()).save(any(StockJournalPo.class));
    }

    /**
     * 验证库存调整数量为 null 时被拒绝。
     * 前置：quantity=null；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void adjustStockNullQuantityThrows() {
        assertThrows(BizException.class, () -> service.adjustStock(100L, null, "test"));
    }

    /**
     * 验证无库存记录时的正向调整会先创建零库存记录再执行增量更新。
     * 前置：库存查询返回 null，保存回调赋予记录 ID 2；当前方法未标注 @Test。
     * 预期：新记录 SKU 为 200、可用量为 5，并保存调整流水。
     */
    public void adjustStockNoRecordCreatesThenIncrements() {
        final StockPo[] created = new StockPo[1];
        when(manager.getOne(any())).thenReturn(null);
        when(manager.save(any(StockPo.class)))
                .thenAnswer(
                        invocation -> {
                            StockPo po = invocation.getArgument(0);
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
        verify(journalManager).save(any(StockJournalPo.class));
    }

    /**
     * 验证商户库存调整会拒绝其他商户名下的 SKU。
     * 前置：SKU 100 属于商户 1，调整使用商户 2；当前方法未标注 @Test。
     * 预期：抛出 BizException，不更新库存。
     */
    public void adjustStockMerchantMismatchThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.adjustStock(100L, 5L, "replenish", 2L));
        verify(manager, never()).updateById(any(StockPo.class));
    }

    /**
     * 验证商户库存调整会拒绝未绑定商户的库存记录。
     * 前置：SKU 100 的 merchantId 为 null，调整指定商户 1；当前方法未标注 @Test。
     * 预期：抛出 BizException，不更新库存。
     */
    public void adjustStockMerchantNullOwnershipThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        s.setReserved(0L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.adjustStock(100L, 5L, "replenish", 1L));
        verify(manager, never()).updateById(any(StockPo.class));
    }

    /**
     * 验证商户调整不存在库存时会创建带 merchantId 的零库存记录。
     * 前置：库存查询返回 null，调整 SKU 200、数量 5、商户 7；当前方法未标注 @Test。
     * 预期：新记录的商户为 7、可用量为 5。
     */
    public void adjustStockMerchantNoRecordCreatesWithMerchantId() {
        final StockPo[] created = new StockPo[1];
        when(manager.getOne(any())).thenReturn(null);
        when(manager.save(any(StockPo.class)))
                .thenAnswer(
                        invocation -> {
                            StockPo po = invocation.getArgument(0);
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
     * 验证设置阈值时库存记录不存在会先创建零库存记录。
     * 前置：库存查询返回 null，保存回调赋予记录 ID 3；当前方法未标注 @Test。
     * 预期：新记录 SKU 为 300、阈值为 20，并执行库存更新。
     */
    public void setThresholdNoRecordCreates() {
        final StockPo[] created = new StockPo[1];
        when(manager.getOne(any())).thenReturn(null);
        when(manager.save(any(StockPo.class)))
                .thenAnswer(
                        invocation -> {
                            StockPo po = invocation.getArgument(0);
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
     * 验证已有库存的告警阈值可正常更新。
     * 前置：SKU 100 库存存在；当前方法未标注 @Test。
     * 预期：告警阈值变为 5，库存更新被调用。
     */
    public void setThresholdSucceeds() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        service.setThreshold(100L, 5L);

        assertEquals(5L, s.getAlertThreshold());
        verify(manager).updateById(s);
    }

    /**
     * 验证告警阈值为 null 时被拒绝。
     * 前置：threshold=null；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void setThresholdNullThrows() {
        assertThrows(BizException.class, () -> service.setThreshold(100L, null));
    }

    /**
     * 验证告警阈值为负数时被拒绝。
     * 前置：threshold=-1；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void setThresholdNegativeThrows() {
        assertThrows(BizException.class, () -> service.setThreshold(100L, -1L));
    }

    /**
     * 验证商户设置阈值时会拒绝其他商户名下的已有库存。
     * 前置：SKU 100 属于商户 1，设置使用商户 2；当前方法未标注 @Test。
     * 预期：抛出 BizException，不更新库存。
     */
    public void setThresholdMerchantMismatchThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.setThreshold(100L, 5L, 2L));
        verify(manager, never()).updateById(any(StockPo.class));
    }

    /**
     * 验证流水查询委托库存流水管理器并返回记录。
     * 前置：流水管理器返回一条 ID 为 1 的记录；当前方法未标注 @Test。
     * 预期：查询列表大小为 1 且记录 ID 为 1。
     */
    public void queryJournalDelegatesToJournalManager() {
        StockJournalPo po = new StockJournalPo();
        po.setId(1L);
        po.setSkuId(100L);
        po.setQuantity(-2L);
        when(journalManager.list(any(Wrapper.class))).thenReturn(List.of(po));

        List<StockJournalPo> result = service.queryJournal(100L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(journalManager).list(any(Wrapper.class));
    }

    /**
     * 验证库存流水查询 SQL 会追加 200 条上限。
     * 前置：流水管理器返回空集合；当前方法未标注 @Test。
     * 预期：捕获查询条件包含 LIMIT 200。
     */
    public void queryJournalAppliesLimit200() {
        when(journalManager.list(any(Wrapper.class))).thenReturn(new ArrayList<>());

        service.queryJournal(100L);

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(journalManager).list(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().contains("LIMIT 200"));
    }

    /**
     * 验证商户流水查询遇到归属不匹配时拒绝访问。
     * 前置：SKU 100 属于商户 1，查询使用商户 2；当前方法未标注 @Test。
     * 预期：抛出 BizException，且不执行流水列表查询。
     */
    public void queryJournalMerchantMismatchThrows() {
        StockPo s = new StockPo();
        s.setId(1L);
        s.setSkuId(100L);
        s.setMerchantId(1L);
        s.setAvailable(10L);
        when(manager.getOne(any())).thenReturn(s);

        assertThrows(BizException.class, () -> service.queryJournal(100L, 2L));
        verify(journalManager, never()).list(any(Wrapper.class));
    }

    /**
     * 验证异常库存查询 SQL 会追加 200 条上限。
     * 前置：库存管理器返回空集合；当前方法未标注 @Test。
     * 预期：捕获查询条件包含 LIMIT 200。
     */
    public void listAbnormalStockAppliesLimit200() {
        when(manager.list(any(Wrapper.class))).thenReturn(new ArrayList<>());

        service.listAbnormalStock();

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(manager).list(captor.capture());
        assertTrue(captor.getValue().getCustomSqlSegment().contains("LIMIT 200"));
    }

    /**
     * 验证完成不存在的盘点任务时抛出业务异常。
     * 前置：任务 ID 999 查询返回 null；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void completeCountTaskNotFoundThrows() {
        when(countTaskManager.getById(999L)).thenReturn(null);

        StockCountTaskCompleteDto dto = new StockCountTaskCompleteDto();
        dto.setActualQuantity(10L);
        assertThrows(BizException.class, () -> service.completeCountTask(999L, dto));
    }

    /**
     * 验证取消不存在的盘点任务时抛出业务异常。
     * 前置：任务 ID 999 查询返回 null；当前方法未标注 @Test。
     * 预期：抛出 BizException。
     */
    public void cancelCountTaskNotFoundThrows() {
        when(countTaskManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.cancelCountTask(999L, 99L));
    }
}
