package com.yirancrazy.minimall.goods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDto;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.AuditDecisionEnum;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuItemDto;
import com.yirancrazy.minimall.goods.dto.SpuCreateDto;
import com.yirancrazy.minimall.goods.dto.SpuPageDto;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDto;
import com.yirancrazy.minimall.goods.entity.SkuPo;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPo;
import com.yirancrazy.minimall.goods.entity.SpuPo;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.impl.SpuServiceImpl;
import com.yirancrazy.minimall.goods.vo.SpuVo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 SPU 生命周期、审核、店铺校验、SKU 同步和 ES 价格区间镜像。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class SpuServiceImplTest {

    private SpuManager spuManager;
    private SpuAuditRecordManager spuAuditRecordManager;
    private SpuSearchService spuSearchService;
    private SkuManager skuManager;
    private GoodsRemoteGateway goodsRemoteGateway;
    private MinioUtil minioUtil;
    private TransactionTemplate transactionTemplate;
    private SpuServiceImpl service;

    @BeforeEach
    void setUp() {
        spuManager = mock(SpuManager.class);
        spuAuditRecordManager = mock(SpuAuditRecordManager.class);
        spuSearchService = mock(SpuSearchService.class);
        skuManager = mock(SkuManager.class);
        goodsRemoteGateway = mock(GoodsRemoteGateway.class);
        minioUtil = mock(MinioUtil.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        lenient()
                .when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus());
        transactionTemplate = new TransactionTemplate(transactionManager);
        lenient()
                .doAnswer(
                        inv -> {
                            SpuPo p = inv.getArgument(0);
                            if (p.getId() == null) {
                                p.setId(System.nanoTime());
                            }
                            return true;
                        })
                .when(spuManager)
                .save(any(SpuPo.class));
        lenient().when(spuManager.updateById(any(SpuPo.class))).thenReturn(true);
        lenient().when(spuManager.removeById(100L)).thenReturn(true);
        lenient().when(skuManager.list(any(Wrapper.class))).thenReturn(List.of());
        lenient().when(spuAuditRecordManager.save(any(SpuAuditRecordPo.class))).thenReturn(true);
        // 默认店铺快照：店铺1归属商家10且营业中，覆盖正常路径
        lenient()
                .when(goodsRemoteGateway.shopSnapshot(anyLong()))
                .thenReturn(Result.success(new ShopSnapshotDto(1L, 10L, "shop-1", "ACTIVE")));
        service =
                new SpuServiceImpl(
                        spuManager,
                        spuAuditRecordManager,
                        spuSearchService,
                        skuManager,
                        goodsRemoteGateway,
                        minioUtil,
                        transactionTemplate);
    }

    /** 验证 SPU 存在时按主键返回对应记录。 */
    public void getByIdReturnsSpuWhenExists() {
        SpuPo s = new SpuPo();
        s.setId(100L);
        s.setTitle("spu-100");
        when(spuManager.getById(100L)).thenReturn(s);

        SpuPo result = service.getById(100L);
        assertEquals(100L, result.getId());
        assertEquals("spu-100", result.getTitle());
    }

    /** 验证 SPU 不存在时按主键查询抛出 SPU_NOT_FOUND。 */
    public void getByIdThrowsWhenMissing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(999L));
    }

    /** 验证商家不匹配时 SPU 查询按不存在处理，防止越权读取。 */
    public void getByIdThrowsWhenMerchantMismatch() {
        SpuPo s = new SpuPo();
        s.setId(100L);
        s.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(s);

        assertThrows(BizException.class, () -> service.getById(100L, 99L));
    }

    /** 验证商家归属匹配时允许读取 SPU。 */
    public void getByIdReturnsSpuWhenOwned() {
        SpuPo s = new SpuPo();
        s.setId(100L);
        s.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(s);

        SpuPo result = service.getById(100L, 10L);
        assertEquals(100L, result.getId());
    }

    /** 验证创建 SPU 返回主键，并写入草稿状态、商家、店铺和商品编号。 */
    public void createReturnsIdAndSetsDraft() {
        SpuCreateDto dto = new SpuCreateDto();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        Long id = service.create(10L, dto);
        assertNotNull(id);

        doAnswer(
                        inv -> {
                            SpuPo p = inv.getArgument(0);
                            assertEquals(SpuStatusEnum.DRAFT.statusValue(), p.getStatus());
                            assertEquals(10L, p.getMerchantId());
                            assertEquals(1L, p.getShopId());
                            assertNotNull(p.getSpuNo());
                            return true;
                        })
                .when(spuManager)
                .save(any(SpuPo.class));

        service.create(10L, dto);
    }

    /** * 验证创建 SPU 时批量保存 SKU，并回填父 SPU 主键和商家归属. */
    @Test
    public void testCreateSavesSkusWithSpuId() {
        SpuCreateDto dto = new SpuCreateDto();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        SkuItemDto item = new SkuItemDto();
        item.setSkuName("颜色/黑");
        item.setPrice(new BigDecimal("19.90"));
        item.setStock(5);
        dto.setSkus(List.of(item));

        service.create(10L, dto);

        ArgumentCaptor<List<SkuPo>> captor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).saveBatch(captor.capture());
        SkuPo saved = captor.getValue().get(0);
        assertEquals(10L, saved.getMerchantId());
        assertEquals("颜色/黑", saved.getSkuName());
        assertEquals(new BigDecimal("19.90"), saved.getPrice());
        assertEquals(5, saved.getStock());
        assertNotNull(saved.getSpuId());
    }

    /** 验证创建 SPU 后将库存初始化延后到事务提交，避免回滚脏调用。 */
    public void createDefersStockInitUntilAfterCommit() {
        SpuCreateDto dto = new SpuCreateDto();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        SkuItemDto item = new SkuItemDto();
        item.setSkuName("颜色/黑");
        item.setPrice(new BigDecimal("19.90"));
        item.setStock(5);
        dto.setSkus(List.of(item));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.create(10L, dto);
            verify(goodsRemoteGateway, never()).initStock(any());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(goodsRemoteGateway).initStock(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /** 验证创建无 SKU 的 SPU 时不调用批量保存。 */
    public void createWithoutSkusSkipsBatchSave() {
        SpuCreateDto dto = new SpuCreateDto();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        service.create(10L, dto);

        verify(skuManager, org.mockito.Mockito.never()).saveBatch(anyList());
    }

    /** 验证店铺主键缺失时拒绝创建 SPU 且不写数据库。 */
    public void createThrowsWhenShopIdMissing() {
        SpuCreateDto dto = new SpuCreateDto();
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        assertThrows(BizException.class, () -> service.create(10L, dto));
        verify(spuManager, org.mockito.Mockito.never()).save(any(SpuPo.class));
    }

    /** 验证店铺不属于当前商家时拒绝创建 SPU。 */
    public void createThrowsWhenShopNotOwned() {
        SpuCreateDto dto = new SpuCreateDto();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        when(goodsRemoteGateway.shopSnapshot(1L))
                .thenReturn(Result.success(new ShopSnapshotDto(1L, 99L, "shop-1", "ACTIVE")));

        assertThrows(BizException.class, () -> service.create(10L, dto));
    }

    /** 验证店铺未营业时拒绝创建 SPU。 */
    public void createThrowsWhenShopInactive() {
        SpuCreateDto dto = new SpuCreateDto();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        when(goodsRemoteGateway.shopSnapshot(1L))
                .thenReturn(Result.success(new ShopSnapshotDto(1L, 10L, "shop-1", "INACTIVE")));

        assertThrows(BizException.class, () -> service.create(10L, dto));
    }

    /** 验证店铺快照无效时拒绝创建 SPU。 */
    public void createThrowsWhenShopServiceDown() {
        SpuCreateDto dto = new SpuCreateDto();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        when(goodsRemoteGateway.shopSnapshot(1L))
                .thenReturn(Result.success(new ShopSnapshotDto(-1L, -1L, "unknown", "DOWN")));

        assertThrows(BizException.class, () -> service.create(10L, dto));
    }

    /** 验证商家读取 SPU 详情时组装所属 SKU 列表。 */
    public void getDetailAssemblesSkus() {
        SpuPo po = new SpuPo();
        po.setId(100L);
        po.setMerchantId(10L);
        po.setTitle("t");
        when(spuManager.getById(100L)).thenReturn(po);
        SkuPo sku = new SkuPo();
        sku.setId(1L);
        sku.setSpuId(100L);
        sku.setSkuName("颜色/黑");
        sku.setPrice(new BigDecimal("19.90"));
        sku.setStock(5);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(sku));

        SpuVo vo = service.getDetail(100L, 10L);

        assertEquals(1, vo.getSkus().size());
        assertEquals("颜色/黑", vo.getSkus().get(0).getSkuName());
        assertEquals(5, vo.getSkus().get(0).getStock());
    }

    /** * 验证 SPU 编辑按明细新增、更新和删除 SKU，并按库存差值调整库存. */
    @Test
    public void testUpdateSyncsSkus() {
        SpuPo po = new SpuPo();
        po.setId(100L);
        po.setMerchantId(10L);
        po.setStatus(SpuStatusEnum.DRAFT.statusValue());
        po.setCreateTime(LocalDateTime.now());
        when(spuManager.getById(100L)).thenReturn(po);

        SkuPo keptSku = new SkuPo();
        keptSku.setId(1L);
        keptSku.setSpuId(100L);
        keptSku.setSkuName("旧规格");
        keptSku.setStock(5);
        SkuPo goneSku = new SkuPo();
        goneSku.setId(2L);
        goneSku.setSpuId(100L);
        goneSku.setSkuName("被移除");
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(keptSku, goneSku));
        when(goodsRemoteGateway.adjustStock(any(StockAdjustDto.class))).thenReturn(Result.success());
        when(skuManager.updateBatchById(anyList())).thenReturn(true);

        final SpuUpdateDto dto = new SpuUpdateDto();
        SkuItemDto kept = new SkuItemDto();
        kept.setId(1L);
        kept.setSkuName("新规格");
        kept.setPrice(new BigDecimal("29.90"));
        kept.setStock(9);
        SkuItemDto added = new SkuItemDto();
        added.setSkuName("新增规格");
        added.setPrice(new BigDecimal("9.90"));
        added.setStock(2);
        dto.setSkus(List.of(kept, added));

        service.update(100L, 10L, dto);

        ArgumentCaptor<List<SkuPo>> createCaptor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).saveBatch(createCaptor.capture());
        assertEquals(1, createCaptor.getValue().size());
        assertEquals("新增规格", createCaptor.getValue().get(0).getSkuName());

        ArgumentCaptor<List<SkuPo>> updateCaptor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).updateBatchById(updateCaptor.capture());
        SkuPo updated = updateCaptor.getValue().get(0);
        assertEquals("新规格", updated.getSkuName());
        assertEquals(new BigDecimal("29.90"), updated.getPrice());
        assertEquals(9, updated.getStock());

        ArgumentCaptor<List<Long>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).removeByIds(deleteCaptor.capture());
        assertEquals(List.of(2L), deleteCaptor.getValue());

        ArgumentCaptor<StockAdjustDto> adjustCaptor = ArgumentCaptor.forClass(StockAdjustDto.class);
        verify(goodsRemoteGateway).adjustStock(adjustCaptor.capture());
        assertEquals(1L, adjustCaptor.getValue().getSkuId());
        assertEquals(4L, adjustCaptor.getValue().getQuantity());
    }

    /** 验证商家 SPU 分页返回记录并组装各 SPU 的 SKU 列表。 */
    public void pageReturnsResults() {
        SpuPo po = new SpuPo();
        po.setId(1L);
        po.setTitle("t");
        List<SpuPo> mockRecords = new ArrayList<>();
        mockRecords.add(po);
        when(spuManager.list(any(Wrapper.class))).thenReturn(mockRecords);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of());

        SpuPageDto dto = new SpuPageDto();
        dto.setLimit(20);
        dto.setMerchantId(10L);
        CursorPageVo<SpuVo> result = service.page(dto);

        assertEquals(1, result.getRecords().size());
        assertEquals("t", result.getRecords().get(0).getTitle());
        assertTrue(result.getRecords().get(0).getSkus().isEmpty());
    }

    /** 验证待更新 SPU 不存在时抛出 SPU_NOT_FOUND。 */
    public void updateThrowsWhenMissing() {
        when(spuManager.getById(999L)).thenReturn(null);
        SpuUpdateDto dto = new SpuUpdateDto();
        dto.setTitle("updated");
        assertThrows(BizException.class, () -> service.update(999L, 10L, dto));
    }

    /** 验证商家不匹配时拒绝更新 SPU。 */
    public void updateThrowsWhenMerchantMismatch() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDto dto = new SpuUpdateDto();
        dto.setTitle("new");
        assertThrows(BizException.class, () -> service.update(100L, 99L, dto));
    }

    /** 验证 SPU 标题和副标题更新成功后返回 true。 */
    public void updateReturnsTrueOnSuccess() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setTitle("old");
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDto dto = new SpuUpdateDto();
        dto.setTitle("new");
        dto.setSubtitle("sub");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals("new", existing.getTitle());
        assertEquals("sub", existing.getSubtitle());
        verify(spuManager).updateById(any(SpuPo.class));
    }

    /** 验证库存调整失败时中止 SPU 编辑且不批量更新 SKU。 */
    public void updateDoesNotSyncSkusWhenStockAdjustFails() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        SkuPo sku = new SkuPo();
        sku.setId(1L);
        sku.setSpuId(100L);
        sku.setSkuName("规格");
        sku.setStock(5);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(sku));
        when(goodsRemoteGateway.adjustStock(any(StockAdjustDto.class)))
                .thenReturn(Result.fail("20000", "stock unavailable"));

        SkuItemDto item = new SkuItemDto();
        item.setId(1L);
        item.setSkuName("规格");
        item.setPrice(new BigDecimal("10.00"));
        item.setStock(6);
        SpuUpdateDto dto = new SpuUpdateDto();
        dto.setSkus(List.of(item));

        BizException ex = assertThrows(BizException.class, () -> service.update(100L, 10L, dto));

        assertEquals(SkuCodeEnum.STOCK_ADJUST_FAILED.getCode(), ex.getCode());
        verify(skuManager, never()).updateBatchById(anyList());
    }

    /** 验证在售 SPU 编辑后回到待审状态，并同步降级后的 ES 文档。 */
    public void updateOnSaleReturnsToPendingAudit() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setTitle("old");
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        existing.setCreateTime(LocalDateTime.now());
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDto dto = new SpuUpdateDto();
        dto.setTitle("new");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
        ArgumentCaptor<SpuDocument> captor = ArgumentCaptor.forClass(SpuDocument.class);
        verify(spuSearchService).sync(captor.capture());
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), captor.getValue().getSaleStatus());
    }

    /** 验证驳回 SPU 编辑后重新进入待审状态。 */
    public void updateRejectedReturnsToPendingAudit() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.REJECTED.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDto dto = new SpuUpdateDto();
        dto.setTitle("revised");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
    }

    /** 验证草稿 SPU 编辑后仍保持草稿状态。 */
    public void updateDraftKeepsDraft() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDto dto = new SpuUpdateDto();
        dto.setTitle("still-draft");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.DRAFT.statusValue(), existing.getStatus());
    }

    /** 验证待删除 SPU 不存在时抛出 SPU_NOT_FOUND。 */
    public void deleteThrowsWhenMissing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.delete(999L, 10L));
    }

    /** 验证商家不匹配时拒绝删除 SPU。 */
    public void deleteThrowsWhenMerchantMismatch() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(100L, 99L));
    }

    /** 验证删除 SPU 成功后返回 true 并移除持久化记录。 */
    public void deleteReturnsTrueOnSuccess() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L, 10L);
        assertTrue(ok);
        verify(spuManager).removeById(100L);
    }

    /** 验证删除 SPU 成功后移除对应 Elasticsearch 文档。 */
    public void deleteRemovesEsDocument() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L, 10L);
        assertTrue(ok);
        verify(spuSearchService).deleteById(100L);
    }

    /** 验证同步 ES 时按 SKU 最低价和最高价写入以分为单位的价格区间。 */
    public void syncToEsFillsPriceRangeFromSkus() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setTitle("priced-spu");
        existing.setCreateTime(LocalDateTime.now());
        when(spuManager.getById(100L)).thenReturn(existing);

        SkuPo low = new SkuPo();
        low.setPrice(new BigDecimal("9.90"));
        SkuPo high = new SkuPo();
        high.setPrice(new BigDecimal("19.90"));
        SkuPo noPrice = new SkuPo();
        noPrice.setPrice(null);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(low, high, noPrice));

        SpuUpdateDto dto = new SpuUpdateDto();
        dto.setTitle("updated");
        service.update(100L, 10L, dto);

        ArgumentCaptor<SpuDocument> captor = ArgumentCaptor.forClass(SpuDocument.class);
        verify(spuSearchService).sync(captor.capture());
        SpuDocument doc = captor.getValue();
        assertEquals(990L, doc.getMinPrice());
        assertEquals(1990L, doc.getMaxPrice());
    }

    /** 验证送审 SPU 不存在时抛出 SPU_NOT_FOUND。 */
    public void onShelfThrowsWhenMissing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.onShelf(999L, 10L));
    }

    /** 验证商家不匹配时拒绝 SPU 送审。 */
    public void onShelfThrowsWhenMerchantMismatch() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.onShelf(100L, 99L));
    }

    /** 验证在售 SPU 不允许重复送审。 */
    public void onShelfThrowsWhenStatusInvalid() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setShopId(1L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.onShelf(100L, 10L));
    }

    /** 验证草稿或驳回 SPU 送审成功后进入待审核状态。 */
    public void onShelfReturnsTrueOnSuccess() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setShopId(1L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.onShelf(100L, 10L);
        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
    }

    /** 验证店铺非营业时拒绝送审且不更新 SPU。 */
    public void onShelfThrowsWhenShopNotActive() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setShopId(1L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);
        when(goodsRemoteGateway.shopSnapshot(1L))
                .thenReturn(Result.success(new ShopSnapshotDto(1L, 10L, "shop-1", "SUSPENDED")));

        assertThrows(BizException.class, () -> service.onShelf(100L, 10L));
        verify(spuManager, org.mockito.Mockito.never()).updateById(any(SpuPo.class));
    }

    /** 验证下架 SPU 不存在时抛出 SPU_NOT_FOUND。 */
    public void offShelfThrowsWhenMissing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.offShelf(999L, 10L));
    }

    /** 验证商家不匹配时拒绝下架 SPU。 */
    public void offShelfThrowsWhenMerchantMismatch() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.offShelf(100L, 99L));
    }

    /** 验证非在售 SPU 不允许下架。 */
    public void offShelfThrowsWhenStatusInvalid() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.offShelf(100L, 10L));
    }

    /** 验证在售 SPU 下架成功后状态变为已下架。 */
    public void offShelfReturnsTrueOnSuccess() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.offShelf(100L, 10L);
        assertTrue(ok);
        assertEquals(SpuStatusEnum.OFF_SHELF.statusValue(), existing.getStatus());
    }

    /** 验证待审核 SPU 游标分页只返回待审记录。 */
    public void pagePendingReturnsResults() {
        List<SpuPo> mockRecords = new ArrayList<>();
        SpuPo spu = new SpuPo();
        spu.setId(100L);
        mockRecords.add(spu);
        when(spuManager.list(any(Wrapper.class))).thenReturn(mockRecords);

        SpuPageDto dto = new SpuPageDto();
        CursorPageVo<SpuPo> result = service.pagePending(dto);
        assertEquals(1, result.getRecords().size());
    }

    /** 验证待审 SPU 审核通过后进入在售、写入发布时间并保存审核记录。 */
    public void approveSuccessSetsOnSaleAndRecordsLog() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.PENDING_AUDIT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.approve(100L, 888L);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.ON_SALE.statusValue(), existing.getStatus());
        assertNotNull(existing.getPublishAt());
        verify(spuAuditRecordManager).save(any(SpuAuditRecordPo.class));
    }

    /** 验证非待审 SPU 不允许审核通过。 */
    public void approveThrowsWhenNotPending() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.approve(100L, 888L));
    }

    /** 验证审核通过时 SPU 不存在抛出业务异常。 */
    public void approveThrowsWhenMissing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.approve(999L, 888L));
    }

    /** 验证待审 SPU 驳回后进入驳回状态并保存原因与审核记录。 */
    public void rejectSuccessSetsRejectedAndRecordsLog() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.PENDING_AUDIT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.reject(100L, 888L, "图片不合规");

        assertTrue(ok);
        assertEquals(SpuStatusEnum.REJECTED.statusValue(), existing.getStatus());
        verify(spuAuditRecordManager).save(any(SpuAuditRecordPo.class));
    }

    /** 验证非待审 SPU 不允许审核驳回。 */
    public void rejectThrowsWhenNotPending() {
        SpuPo existing = new SpuPo();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reject(100L, 888L, "reason"));
    }

    /** 验证按 SPU 主键返回审核记录列表。 */
    public void listAuditRecordsReturnsRecords() {
        SpuAuditRecordPo rec = new SpuAuditRecordPo();
        rec.setSpuId(100L);
        rec.setDecision(AuditDecisionEnum.APPROVE.getCode());
        when(spuAuditRecordManager.list(any(Wrapper.class))).thenReturn(List.of(rec));

        List<SpuAuditRecordPo> records = service.listAuditRecords(100L);

        assertEquals(1, records.size());
        assertEquals(AuditDecisionEnum.APPROVE.getCode(), records.get(0).getDecision());
    }

    /** 验证批量 SPU 快照以主键为键返回已存在记录，缺失主键不出现。 */
    public void listSnapshotsReturnsMap() {
        SpuPo spu1 = new SpuPo();
        spu1.setId(1L);
        spu1.setTitle("标题一");
        spu1.setMainImageUrl("http://img/1.jpg");
        SpuPo spu2 = new SpuPo();
        spu2.setId(2L);
        spu2.setTitle("标题二");
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of(spu1, spu2));

        Map<Long, SpuSnapshotDto> result = service.listSnapshots(List.of(1L, 2L, 3L));

        assertEquals(2, result.size());
        assertEquals("标题一", result.get(1L).getTitle());
        assertEquals("http://img/1.jpg", result.get(1L).getMainImageUrl());
        assertEquals("标题二", result.get(2L).getTitle());
        assertNull(result.get(3L));
    }

    /** 验证空 SPU 主键列表返回空快照映射。 */
    public void listSnapshotsEmptyReturnsEmptyMap() {
        assertTrue(service.listSnapshots(List.of()).isEmpty());
    }
}
