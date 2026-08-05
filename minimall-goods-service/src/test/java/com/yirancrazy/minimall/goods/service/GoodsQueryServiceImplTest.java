package com.yirancrazy.minimall.goods.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
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
    private GoodsQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        spuManager = mock(SpuManager.class);
        skuManager = mock(SkuManager.class);
        service = new GoodsQueryServiceImpl(spuManager, skuManager);
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

        GoodsPageDTO dto = new GoodsPageDTO();

        CursorPageVO<SpuListVO> result = service.pageOnSale(dto);

        assertEquals(1, result.getRecords().size());
        SpuListVO vo = result.getRecords().get(0);
        assertEquals(200L, vo.getSpuId());
        assertEquals("手机", vo.getTitle());
        assertEquals(10L, vo.getMerchantId());
    }

    /**
     * 验证 pageOnSale 支持关键词与分类过滤且返回空列表不报错。
     */
    @Test
    public void pageOnSale_with_keyword_and_category_returns_empty() {
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of());

        GoodsPageDTO dto = new GoodsPageDTO();
        dto.setKeyword("耳机");
        dto.setCategoryId(5L);

        var result = service.pageOnSale(dto);

        assertTrue(result.getRecords().isEmpty());
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
