package com.yirancrazy.minimall.goods.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.GoodsPageDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.service.impl.GoodsQueryServiceImpl;
import com.yirancrazy.minimall.goods.vo.SpuDetailVO;
import com.yirancrazy.minimall.goods.vo.SpuListVO;

/**
 * GoodsQueryServiceImpl 单元测试，覆盖用户端在售商品列表、详情、SKU 列表查询的正常与失败路径。
 */
public class GoodsQueryServiceImplTest {

    private SpuManager spuManager;
    private SkuManager skuManager;
    private MinioUtil minioUtil;
    private GoodsQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        spuManager = mock(SpuManager.class);
        skuManager = mock(SkuManager.class);
        minioUtil = mock(MinioUtil.class);
        service = new GoodsQueryServiceImpl(spuManager, skuManager, minioUtil);
    }

    /**
     * 验证 pageOnSale 在有在售商品时返回映射后的列表 VO。
     */
    @Test
    public void pageOnSale_returns_results() {
        SpuPO po = new SpuPO();
        po.setId(200L);
        po.setSpuNo("SPU200");
        po.setTitle("手机");
        po.setMerchantId(10L);
        List<SpuPO> mockRecords = new ArrayList<>();
        mockRecords.add(po);
        when(spuManager.list(any(Wrapper.class))).thenReturn(mockRecords);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of());

        GoodsPageDTO dto = new GoodsPageDTO();

        CursorPageVO<SpuListVO> result = service.pageOnSale(dto);

        assertEquals(1, result.getRecords().size());
        SpuListVO vo = result.getRecords().get(0);
        assertEquals(200L, vo.getSpuId());
        assertEquals("手机", vo.getTitle());
        assertEquals(10L, vo.getMerchantId());
        assertNull(vo.getMinPrice());
    }

    /**
     * 验证 pageOnSale 将裸 objectKey 主图转成预签名 URL，空主图返回 null。
     */
    @Test
    public void pageOnSale_resolves_object_key_to_presigned_url() {
        SpuPO po = new SpuPO();
        po.setId(300L);
        po.setSpuNo("SPU300");
        po.setTitle("耳机");
        po.setMainImageUrl("a1b2c3.png");
        po.setMerchantId(10L);
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of(po));
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of());
        when(minioUtil.resolvePublicUrl("a1b2c3.png")).thenReturn("http://minio/mall-files/a1b2c3.png?token");

        var result = service.pageOnSale(new GoodsPageDTO());

        assertEquals("http://minio/mall-files/a1b2c3.png?token", result.getRecords().get(0).getMainImageUrl());
    }

    /**
     * 验证 pageOnSale 支持关键词与分类过滤且返回空列表不报错。
     */
    @Test
    public void pageOnSale_with_keyword_and_category_returns_empty() {
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of());
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of());

        GoodsPageDTO dto = new GoodsPageDTO();
        dto.setKeyword("耳机");
        dto.setCategoryId(5L);

        var result = service.pageOnSale(dto);

        assertTrue(result.getRecords().isEmpty());
    }

    /**
     * 验证 pageOnSale 批量聚合各 SPU 最低售价，无 SKU 的 SPU 返回 null。
     */
    @Test
    public void pageOnSale_aggregates_min_price() {
        SpuPO po1 = new SpuPO();
        po1.setId(1L);
        po1.setSpuNo("SPU1");
        po1.setTitle("商品一");
        po1.setMerchantId(10L);
        SpuPO po2 = new SpuPO();
        po2.setId(2L);
        po2.setSpuNo("SPU2");
        po2.setTitle("商品二");
        po2.setMerchantId(10L);
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of(po1, po2));

        SkuPO s1 = new SkuPO();
        s1.setSpuId(1L);
        s1.setPrice(new BigDecimal("199.00"));
        SkuPO s2 = new SkuPO();
        s2.setSpuId(1L);
        s2.setPrice(new BigDecimal("99.00"));
        SkuPO s3 = new SkuPO();
        s3.setSpuId(2L);
        s3.setPrice(new BigDecimal("59.00"));
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(s1, s2, s3));

        GoodsPageDTO dto = new GoodsPageDTO();

        var result = service.pageOnSale(dto);

        assertEquals(2, result.getRecords().size());
        assertEquals(new BigDecimal("99.00"), result.getRecords().get(0).getMinPrice());
        assertEquals(new BigDecimal("59.00"), result.getRecords().get(1).getMinPrice());
    }

    /**
     * 验证 pageOnSale 批量聚合最低价时跳过空价 SKU：混有空价的 SPU 取非空最小值，
     * 全空价 SKU 的 SPU 最低价为 null，且不抛 NPE。
     */
    @Test
    public void pageOnSale_skips_null_price_when_aggregating() {
        SpuPO po1 = new SpuPO();
        po1.setId(1L);
        po1.setSpuNo("SPU1");
        po1.setTitle("商品一");
        po1.setMerchantId(10L);
        SpuPO po2 = new SpuPO();
        po2.setId(2L);
        po2.setSpuNo("SPU2");
        po2.setTitle("商品二");
        po2.setMerchantId(10L);
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of(po1, po2));

        SkuPO s1 = new SkuPO();
        s1.setSpuId(1L);
        s1.setPrice(new BigDecimal("199.00"));
        SkuPO s2 = new SkuPO();
        s2.setSpuId(1L);
        s2.setPrice(null);
        SkuPO s3 = new SkuPO();
        s3.setSpuId(2L);
        s3.setPrice(null);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(s1, s2, s3));

        GoodsPageDTO dto = new GoodsPageDTO();

        var result = service.pageOnSale(dto);

        assertEquals(2, result.getRecords().size());
        assertEquals(new BigDecimal("199.00"), result.getRecords().get(0).getMinPrice());
        assertNull(result.getRecords().get(1).getMinPrice());
    }

    /**
     * 验证 getDetail 在 SPU 在售时聚合返回详情与 SKU 列表。
     */
    @Test
    public void getDetail_success_with_skus() {
        SpuPO po = new SpuPO();
        po.setId(200L);
        po.setSpuNo("SPU200");
        po.setTitle("手机");
        po.setSubtitle("旗舰机");
        po.setMainImageUrl("http://img");
        po.setMerchantId(10L);
        po.setCategoryId(1L);
        po.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(200L)).thenReturn(po);

        SkuPO sku = new SkuPO();
        sku.setId(300L);
        sku.setSpuId(200L);
        sku.setSkuName("黑色 128G");
        sku.setPrice(new BigDecimal("1999.00"));
        sku.setStock(50);
        lenient().when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(sku));

        SpuDetailVO detail = service.getDetail(200L);

        assertEquals(200L, detail.getSpuId());
        assertEquals("手机", detail.getTitle());
        assertTrue(detail.getIsOnSale());
        assertEquals(1, detail.getSkus().size());
        assertEquals(300L, detail.getSkus().get(0).getId());
        assertEquals(new BigDecimal("1999.00"), detail.getSkus().get(0).getPrice());
    }

    /**
     * 验证 getDetail 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void getDetail_throws_when_spu_not_found() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getDetail(999L));
    }

    /**
     * 验证 getDetail 在 SPU 非在售时对用户不可见，抛出 BizException。
     */
    @Test
    public void getDetail_throws_when_not_on_sale() {
        SpuPO po = new SpuPO();
        po.setId(201L);
        po.setStatus(SpuStatusEnum.OFF_SHELF.statusValue());
        when(spuManager.getById(201L)).thenReturn(po);
        assertThrows(BizException.class, () -> service.getDetail(201L));
    }

    /**
     * 验证 listSkus 在 SPU 在售时返回 SKU 视图列表。
     */
    @Test
    public void listSkus_success() {
        SpuPO po = new SpuPO();
        po.setId(200L);
        po.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(200L)).thenReturn(po);

        SkuPO s1 = new SkuPO();
        s1.setId(301L);
        s1.setSpuId(200L);
        s1.setSkuName("黑色");
        s1.setPrice(new BigDecimal("99.00"));
        s1.setStock(10);
        SkuPO s2 = new SkuPO();
        s2.setId(302L);
        s2.setSpuId(200L);
        s2.setSkuName("白色");
        s2.setPrice(new BigDecimal("109.00"));
        s2.setStock(20);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(s1, s2));

        var skus = service.listSkus(200L);

        assertNotNull(skus);
        assertEquals(2, skus.size());
        assertEquals(301L, skus.get(0).getId());
        assertEquals("白色", skus.get(1).getSkuName());
    }

    /**
     * 验证 listSkus 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void listSkus_throws_when_spu_not_found() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.listSkus(999L));
    }
}
