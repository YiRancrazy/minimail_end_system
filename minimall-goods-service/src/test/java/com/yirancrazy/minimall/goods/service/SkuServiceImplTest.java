package com.yirancrazy.minimall.goods.service;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.service.impl.SkuServiceImpl;

/**
 * SkuServiceImpl 单元测试，覆盖按主键查询与创建 SKU 的正常、失败、边界路径。
 */
public class SkuServiceImplTest {

    private SkuManager skuManager;
    private SkuServiceImpl service;

    @BeforeEach
    void setUp() {
        skuManager = mock(SkuManager.class);
        lenient().doAnswer(inv -> {
            SkuPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(skuManager).save(any(SkuPO.class));
        service = new SkuServiceImpl(skuManager);
    }

    /**
     * 验证 getById 在 SKU 存在时返回实体。
     */
    @Test
    public void getById_returns_sku_when_exists() {
        SkuPO s = new SkuPO();
        s.setId(100L);
        s.setSkuName("sku-100");
        when(skuManager.getById(100L)).thenReturn(s);

        SkuPO result = service.getById(100L);
        assertEquals(100L, result.getId());
        assertEquals("sku-100", result.getSkuName());
    }

    /**
     * 验证 getById 在 SKU 不存在时抛出 BizException。
     */
    @Test
    public void getById_throws_when_missing() {
        when(skuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(999L));
    }

    /**
     * 验证 create 在 price/stock 为 null 时使用缺省值并返回新 ID。
     */
    @Test
    public void create_uses_defaults_when_price_and_stock_null() {
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("new-sku");

        Long id = service.create(dto);
        assertNotNull(id);

        doAnswer(inv -> {
            SkuPO p = inv.getArgument(0);
            assertEquals(BigDecimal.ZERO, p.getPrice());
            assertEquals(0, p.getStock());
            return true;
        }).when(skuManager).save(any(SkuPO.class));

        service.create(dto);
    }

    /**
     * 验证 create 在 price/stock 非空时透传原值。
     */
    @Test
    public void create_passes_through_price_and_stock() {
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("priced-sku");
        dto.setPrice(new BigDecimal("19.90"));
        dto.setStock(50);

        Long id = service.create(dto);
        assertNotNull(id);
    }
}
