package com.yirancrazy.minimall.goods.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.entity.SkuPo;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.service.impl.SkuStockSyncServiceImpl;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证库存缓存分批同步、缺失库存归零和远程失败异常传播。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 */
public class SkuStockSyncServiceImplTest {

    private SkuManager skuManager;
    private GoodsRemoteGateway goodsRemoteGateway;
    private SkuStockSyncServiceImpl service;

    /** 初始化被测对象及其 Mock 依赖。 */
    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.goods.mapper.SkuMapper");
        TableInfoHelper.initTableInfo(assistant, SkuPo.class);

        skuManager = mock(SkuManager.class);
        goodsRemoteGateway = mock(GoodsRemoteGateway.class);
        service = new SkuStockSyncServiceImpl(skuManager, goodsRemoteGateway, 100);
    }

    /** * 验证缓存同步更新可用量变化的 SKU，并将缺失权威库存的 SKU 归零. */
    @Test
    void testSyncStockCacheUpdatesChangedAndMissingStock() {
        SkuPo first = sku(1L, 10);
        SkuPo second = sku(2L, 5);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(first, second), List.of());
        when(skuManager.updateBatchById(anyList())).thenReturn(true);
        when(goodsRemoteGateway.queryAvailability(any()))
                .thenReturn(Result.success(List.of(new StockAvailabilityDto(1L, 7L))));

        int updated = service.syncStockCache();

        assertEquals(2, updated);
        assertEquals(7, first.getStock());
        assertEquals(0, second.getStock());
        verify(skuManager).updateBatchById(anyList());
    }

    /** * 验证库存服务返回失败时同步任务抛出系统业务异常. */
    @Test
    void testSyncStockCacheRemoteFailureThrows() {
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(sku(1L, 10)));
        when(goodsRemoteGateway.queryAvailability(any()))
                .thenReturn(Result.fail(CommonCode.SYS_ERROR, "库存服务不可用"));

        assertThrows(BizException.class, () -> service.syncStockCache());
    }

    private SkuPo sku(Long id, Integer stock) {
        SkuPo sku = new SkuPo();
        sku.setId(id);
        sku.setStock(stock);
        return sku;
    }
}
