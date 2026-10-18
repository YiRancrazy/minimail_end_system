package com.yirancrazy.minimall.goods.service;

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
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.AuditDecisionEnum;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuItemDTO;
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.impl.SpuServiceImpl;
import com.yirancrazy.minimall.goods.vo.SpuVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
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
        lenient().when(transactionManager.getTransaction(any()))
            .thenReturn(new SimpleTransactionStatus());
        transactionTemplate = new TransactionTemplate(transactionManager);
        lenient().doAnswer(inv -> {
            SpuPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(spuManager).save(any(SpuPO.class));
        lenient().when(spuManager.updateById(any(SpuPO.class))).thenReturn(true);
        lenient().when(spuManager.removeById(100L)).thenReturn(true);
        lenient().when(skuManager.list(any(Wrapper.class))).thenReturn(List.of());
        lenient().when(spuAuditRecordManager.save(any(SpuAuditRecordPO.class))).thenReturn(true);
        // 默认店铺快照：店铺1归属商家10且营业中，覆盖正常路径
        lenient().when(goodsRemoteGateway.shopSnapshot(anyLong()))
            .thenReturn(Result.success(new ShopSnapshotDTO(1L, 10L, "shop-1", "ACTIVE")));
        service = new SpuServiceImpl(spuManager, spuAuditRecordManager,
            spuSearchService, skuManager, goodsRemoteGateway, minioUtil, transactionTemplate);
    }


    /**
     * 获取相关数据。
     */
    public void getById_returns_spu_when_exists() {
        SpuPO s = new SpuPO();
        s.setId(100L);
        s.setTitle("spu-100");
        when(spuManager.getById(100L)).thenReturn(s);

        SpuPO result = service.getById(100L);
        assertEquals(100L, result.getId());
        assertEquals("spu-100", result.getTitle());
    }


    /**
     * 获取相关数据。
     */
    public void getById_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(999L));
    }


    /**
     * 获取相关数据。
     */
    public void getById_throws_when_merchant_mismatch() {
        SpuPO s = new SpuPO();
        s.setId(100L);
        s.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(s);

        assertThrows(BizException.class, () -> service.getById(100L, 99L));
    }


    /**
     * 获取相关数据。
     */
    public void getById_returns_spu_when_owned() {
        SpuPO s = new SpuPO();
        s.setId(100L);
        s.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(s);

        SpuPO result = service.getById(100L, 10L);
        assertEquals(100L, result.getId());
    }


    /**
     * 创建业务数据。
     */
    public void create_returns_id_and_sets_draft() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        Long id = service.create(10L, dto);
        assertNotNull(id);

        doAnswer(inv -> {
            SpuPO p = inv.getArgument(0);
            assertEquals(SpuStatusEnum.DRAFT.statusValue(), p.getStatus());
            assertEquals(10L, p.getMerchantId());
            assertEquals(1L, p.getShopId());
            assertNotNull(p.getSpuNo());
            return true;
        }).when(spuManager).save(any(SpuPO.class));

        service.create(10L, dto);
    }


    /**
     * 创建业务数据。
     */
    @Test
    public void create_saves_skus_with_spu_id() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        SkuItemDTO item = new SkuItemDTO();
        item.setSkuName("颜色/黑");
        item.setPrice(new BigDecimal("19.90"));
        item.setStock(5);
        dto.setSkus(List.of(item));

        service.create(10L, dto);

        ArgumentCaptor<List<SkuPO>> captor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).saveBatch(captor.capture());
        SkuPO saved = captor.getValue().get(0);
        assertEquals(10L, saved.getMerchantId());
        assertEquals("颜色/黑", saved.getSkuName());
        assertEquals(new BigDecimal("19.90"), saved.getPrice());
        assertEquals(5, saved.getStock());
        assertNotNull(saved.getSpuId());
    }


    /**
     * 创建业务数据。
     */
    public void create_defers_stock_init_until_after_commit() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        SkuItemDTO item = new SkuItemDTO();
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
        }
        finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }


    /**
     * 创建业务数据。
     */
    public void create_without_skus_skips_batch_save() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        service.create(10L, dto);

        verify(skuManager, org.mockito.Mockito.never()).saveBatch(anyList());
    }


    /**
     * 创建业务数据。
     */
    public void create_throws_when_shop_id_missing() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        assertThrows(BizException.class, () -> service.create(10L, dto));
        verify(spuManager, org.mockito.Mockito.never()).save(any(SpuPO.class));
    }


    /**
     * 创建业务数据。
     */
    public void create_throws_when_shop_not_owned() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        when(goodsRemoteGateway.shopSnapshot(1L))
            .thenReturn(Result.success(new ShopSnapshotDTO(1L, 99L, "shop-1", "ACTIVE")));

        assertThrows(BizException.class, () -> service.create(10L, dto));
    }


    /**
     * 创建业务数据。
     */
    public void create_throws_when_shop_inactive() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        when(goodsRemoteGateway.shopSnapshot(1L))
            .thenReturn(Result.success(new ShopSnapshotDTO(1L, 10L, "shop-1", "INACTIVE")));

        assertThrows(BizException.class, () -> service.create(10L, dto));
    }


    /**
     * 创建业务数据。
     */
    public void create_throws_when_shop_service_down() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        when(goodsRemoteGateway.shopSnapshot(1L))
            .thenReturn(Result.success(new ShopSnapshotDTO(-1L, -1L, "unknown", "DOWN")));

        assertThrows(BizException.class, () -> service.create(10L, dto));
    }


    /**
     * 获取相关数据。
     */
    public void getDetail_assembles_skus() {
        SpuPO po = new SpuPO();
        po.setId(100L);
        po.setMerchantId(10L);
        po.setTitle("t");
        when(spuManager.getById(100L)).thenReturn(po);
        SkuPO sku = new SkuPO();
        sku.setId(1L);
        sku.setSpuId(100L);
        sku.setSkuName("颜色/黑");
        sku.setPrice(new BigDecimal("19.90"));
        sku.setStock(5);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(sku));

        SpuVO vo = service.getDetail(100L, 10L);

        assertEquals(1, vo.getSkus().size());
        assertEquals("颜色/黑", vo.getSkus().get(0).getSkuName());
        assertEquals(5, vo.getSkus().get(0).getStock());
    }


    /**
     * 更新业务数据。
     */
    @Test
    public void update_syncs_skus() {
        SpuPO po = new SpuPO();
        po.setId(100L);
        po.setMerchantId(10L);
        po.setStatus(SpuStatusEnum.DRAFT.statusValue());
        po.setCreateTime(LocalDateTime.now());
        when(spuManager.getById(100L)).thenReturn(po);

        SkuPO keptSku = new SkuPO();
        keptSku.setId(1L);
        keptSku.setSpuId(100L);
        keptSku.setSkuName("旧规格");
        keptSku.setStock(5);
        SkuPO goneSku = new SkuPO();
        goneSku.setId(2L);
        goneSku.setSpuId(100L);
        goneSku.setSkuName("被移除");
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(keptSku, goneSku));
        when(goodsRemoteGateway.adjustStock(any(StockAdjustDTO.class))).thenReturn(Result.success());
        when(skuManager.updateBatchById(anyList())).thenReturn(true);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        SkuItemDTO kept = new SkuItemDTO();
        kept.setId(1L);
        kept.setSkuName("新规格");
        kept.setPrice(new BigDecimal("29.90"));
        kept.setStock(9);
        SkuItemDTO added = new SkuItemDTO();
        added.setSkuName("新增规格");
        added.setPrice(new BigDecimal("9.90"));
        added.setStock(2);
        dto.setSkus(List.of(kept, added));

        service.update(100L, 10L, dto);

        ArgumentCaptor<List<SkuPO>> createCaptor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).saveBatch(createCaptor.capture());
        assertEquals(1, createCaptor.getValue().size());
        assertEquals("新增规格", createCaptor.getValue().get(0).getSkuName());

        ArgumentCaptor<List<SkuPO>> updateCaptor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).updateBatchById(updateCaptor.capture());
        SkuPO updated = updateCaptor.getValue().get(0);
        assertEquals("新规格", updated.getSkuName());
        assertEquals(new BigDecimal("29.90"), updated.getPrice());
        assertEquals(9, updated.getStock());

        ArgumentCaptor<List<Long>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).removeByIds(deleteCaptor.capture());
        assertEquals(List.of(2L), deleteCaptor.getValue());

        ArgumentCaptor<StockAdjustDTO> adjustCaptor = ArgumentCaptor.forClass(StockAdjustDTO.class);
        verify(goodsRemoteGateway).adjustStock(adjustCaptor.capture());
        assertEquals(1L, adjustCaptor.getValue().getSkuId());
        assertEquals(4L, adjustCaptor.getValue().getQuantity());
    }


    /**
     * 分页查询数据。
     */
    public void page_returns_results() {
        SpuPO po = new SpuPO();
        po.setId(1L);
        po.setTitle("t");
        List<SpuPO> mockRecords = new ArrayList<>();
        mockRecords.add(po);
        when(spuManager.list(any(Wrapper.class))).thenReturn(mockRecords);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of());

        SpuPageDTO dto = new SpuPageDTO();
        dto.setLimit(20);
        dto.setMerchantId(10L);
        CursorPageVO<SpuVO> result = service.page(dto);

        assertEquals(1, result.getRecords().size());
        assertEquals("t", result.getRecords().get(0).getTitle());
        assertTrue(result.getRecords().get(0).getSkus().isEmpty());
    }


    /**
     * 更新业务数据。
     */
    public void update_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("updated");
        assertThrows(BizException.class, () -> service.update(999L, 10L, dto));
    }


    /**
     * 更新业务数据。
     */
    public void update_throws_when_merchant_mismatch() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("new");
        assertThrows(BizException.class, () -> service.update(100L, 99L, dto));
    }


    /**
     * 更新业务数据。
     */
    public void update_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setTitle("old");
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("new");
        dto.setSubtitle("sub");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals("new", existing.getTitle());
        assertEquals("sub", existing.getSubtitle());
        verify(spuManager).updateById(any(SpuPO.class));
    }


    /**
     * 更新业务数据。
     */
    public void update_does_not_sync_skus_when_stock_adjust_fails() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        SkuPO sku = new SkuPO();
        sku.setId(1L);
        sku.setSpuId(100L);
        sku.setSkuName("规格");
        sku.setStock(5);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(sku));
        when(goodsRemoteGateway.adjustStock(any(StockAdjustDTO.class)))
            .thenReturn(Result.fail("20000", "stock unavailable"));

        SkuItemDTO item = new SkuItemDTO();
        item.setId(1L);
        item.setSkuName("规格");
        item.setPrice(new BigDecimal("10.00"));
        item.setStock(6);
        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setSkus(List.of(item));

        BizException ex = assertThrows(BizException.class,
            () -> service.update(100L, 10L, dto));

        assertEquals(SkuCodeEnum.STOCK_ADJUST_FAILED.getCode(), ex.getCode());
        verify(skuManager, never()).updateBatchById(anyList());
    }


    /**
     * 更新业务数据。
     */
    public void update_on_sale_returns_to_pending_audit() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setTitle("old");
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        existing.setCreateTime(LocalDateTime.now());
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("new");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
        ArgumentCaptor<SpuDocument> captor = ArgumentCaptor.forClass(SpuDocument.class);
        verify(spuSearchService).sync(captor.capture());
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(),
            captor.getValue().getSaleStatus());
    }


    /**
     * 更新业务数据。
     */
    public void update_rejected_returns_to_pending_audit() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.REJECTED.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("revised");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
    }


    /**
     * 更新业务数据。
     */
    public void update_draft_keeps_draft() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("still-draft");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.DRAFT.statusValue(), existing.getStatus());
    }


    /**
     * 删除业务数据。
     */
    public void delete_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.delete(999L, 10L));
    }


    /**
     * 删除业务数据。
     */
    public void delete_throws_when_merchant_mismatch() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(100L, 99L));
    }


    /**
     * 删除业务数据。
     */
    public void delete_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L, 10L);
        assertTrue(ok);
        verify(spuManager).removeById(100L);
    }


    /**
     * 删除业务数据。
     */
    public void delete_removes_es_document() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L, 10L);
        assertTrue(ok);
        verify(spuSearchService).deleteById(100L);
    }


    /**
     * 执行 sync_to_es_fills_price_range_from_skus 方法。
     */
    public void sync_to_es_fills_price_range_from_skus() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setTitle("priced-spu");
        existing.setCreateTime(LocalDateTime.now());
        when(spuManager.getById(100L)).thenReturn(existing);

        SkuPO low = new SkuPO();
        low.setPrice(new BigDecimal("9.90"));
        SkuPO high = new SkuPO();
        high.setPrice(new BigDecimal("19.90"));
        SkuPO noPrice = new SkuPO();
        noPrice.setPrice(null);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(low, high, noPrice));

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("updated");
        service.update(100L, 10L, dto);

        ArgumentCaptor<SpuDocument> captor = ArgumentCaptor.forClass(SpuDocument.class);
        verify(spuSearchService).sync(captor.capture());
        SpuDocument doc = captor.getValue();
        assertEquals(990L, doc.getMinPrice());
        assertEquals(1990L, doc.getMaxPrice());
    }


    /**
     * 执行 onShelf_throws_when_missing 方法。
     */
    public void onShelf_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.onShelf(999L, 10L));
    }


    /**
     * 执行 onShelf_throws_when_merchant_mismatch 方法。
     */
    public void onShelf_throws_when_merchant_mismatch() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.onShelf(100L, 99L));
    }


    /**
     * 执行 onShelf_throws_when_status_invalid 方法。
     */
    public void onShelf_throws_when_status_invalid() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setShopId(1L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.onShelf(100L, 10L));
    }


    /**
     * 执行 onShelf_returns_true_on_success 方法。
     */
    public void onShelf_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setShopId(1L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.onShelf(100L, 10L);
        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
    }


    /**
     * 执行 onShelf_throws_when_shop_not_active 方法。
     */
    public void onShelf_throws_when_shop_not_active() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setShopId(1L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);
        when(goodsRemoteGateway.shopSnapshot(1L))
            .thenReturn(Result.success(new ShopSnapshotDTO(1L, 10L, "shop-1", "SUSPENDED")));

        assertThrows(BizException.class, () -> service.onShelf(100L, 10L));
        verify(spuManager, org.mockito.Mockito.never()).updateById(any(SpuPO.class));
    }


    /**
     * 执行 offShelf_throws_when_missing 方法。
     */
    public void offShelf_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.offShelf(999L, 10L));
    }


    /**
     * 执行 offShelf_throws_when_merchant_mismatch 方法。
     */
    public void offShelf_throws_when_merchant_mismatch() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.offShelf(100L, 99L));
    }


    /**
     * 执行 offShelf_throws_when_status_invalid 方法。
     */
    public void offShelf_throws_when_status_invalid() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.offShelf(100L, 10L));
    }


    /**
     * 执行 offShelf_returns_true_on_success 方法。
     */
    public void offShelf_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.offShelf(100L, 10L);
        assertTrue(ok);
        assertEquals(SpuStatusEnum.OFF_SHELF.statusValue(), existing.getStatus());
    }


    /**
     * 分页查询数据。
     */
    public void pagePending_returns_results() {
        List<SpuPO> mockRecords = new ArrayList<>();
        SpuPO spu = new SpuPO();
        spu.setId(100L);
        mockRecords.add(spu);
        when(spuManager.list(any(Wrapper.class))).thenReturn(mockRecords);

        SpuPageDTO dto = new SpuPageDTO();
        CursorPageVO<SpuPO> result = service.pagePending(dto);
        assertEquals(1, result.getRecords().size());
    }


    /**
     * 执行 approve_success_sets_on_sale_and_records_log 方法。
     */
    public void approve_success_sets_on_sale_and_records_log() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.PENDING_AUDIT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.approve(100L, 888L);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.ON_SALE.statusValue(), existing.getStatus());
        assertNotNull(existing.getPublishAt());
        verify(spuAuditRecordManager).save(any(SpuAuditRecordPO.class));
    }


    /**
     * 执行 approve_throws_when_not_pending 方法。
     */
    public void approve_throws_when_not_pending() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.approve(100L, 888L));
    }


    /**
     * 执行 approve_throws_when_missing 方法。
     */
    public void approve_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.approve(999L, 888L));
    }


    /**
     * 执行 reject_success_sets_rejected_and_records_log 方法。
     */
    public void reject_success_sets_rejected_and_records_log() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.PENDING_AUDIT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.reject(100L, 888L, "图片不合规");

        assertTrue(ok);
        assertEquals(SpuStatusEnum.REJECTED.statusValue(), existing.getStatus());
        verify(spuAuditRecordManager).save(any(SpuAuditRecordPO.class));
    }


    /**
     * 执行 reject_throws_when_not_pending 方法。
     */
    public void reject_throws_when_not_pending() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reject(100L, 888L, "reason"));
    }


    /**
     * 查询列表数据。
     */
    public void listAuditRecords_returns_records() {
        SpuAuditRecordPO rec = new SpuAuditRecordPO();
        rec.setSpuId(100L);
        rec.setDecision(AuditDecisionEnum.APPROVE.getCode());
        when(spuAuditRecordManager.list(any(Wrapper.class))).thenReturn(List.of(rec));

        List<SpuAuditRecordPO> records = service.listAuditRecords(100L);

        assertEquals(1, records.size());
        assertEquals(AuditDecisionEnum.APPROVE.getCode(), records.get(0).getDecision());
    }


    /**
     * 查询列表数据。
     */
    public void listSnapshots_returns_map() {
        SpuPO spu1 = new SpuPO();
        spu1.setId(1L);
        spu1.setTitle("标题一");
        spu1.setMainImageUrl("http://img/1.jpg");
        SpuPO spu2 = new SpuPO();
        spu2.setId(2L);
        spu2.setTitle("标题二");
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of(spu1, spu2));

        Map<Long, SpuSnapshotDTO> result = service.listSnapshots(List.of(1L, 2L, 3L));

        assertEquals(2, result.size());
        assertEquals("标题一", result.get(1L).getTitle());
        assertEquals("http://img/1.jpg", result.get(1L).getMainImageUrl());
        assertEquals("标题二", result.get(2L).getTitle());
        assertNull(result.get(3L));
    }


    /**
     * 查询列表数据。
     */
    public void listSnapshots_empty_returns_empty_map() {
        assertTrue(service.listSnapshots(List.of()).isEmpty());
    }
}
