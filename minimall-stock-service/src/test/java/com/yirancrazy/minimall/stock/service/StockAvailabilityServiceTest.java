package com.yirancrazy.minimall.stock.service;

import java.util.Arrays;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDTO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.manager.StockCountTaskManager;
import com.yirancrazy.minimall.stock.manager.StockJournalManager;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.manager.StockTransferManager;
import com.yirancrazy.minimall.stock.service.impl.StockServiceImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存可用量批量查询单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 **/
public class StockAvailabilityServiceTest {

    private StockManager stockManager;
    private StockServiceImpl service;

    /**
     * 初始化测试依赖。
     */
    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.stock.mapper.StockMapper");
        TableInfoHelper.initTableInfo(assistant, StockPO.class);

        stockManager = mock(StockManager.class);
        service = new StockServiceImpl(
            stockManager,
            mock(StockJournalManager.class),
            mock(StockTransferManager.class),
            mock(StockCountTaskManager.class)
        );
    }

    /**
     * 验证批量查询去重、保持请求顺序并为零库存填充默认值。
     */
    @Test
    void queryAvailability_returnsDistinctRequestedSkus() {
        StockPO stock = new StockPO();
        stock.setSkuId(1L);
        stock.setAvailable(8L);
        when(stockManager.list(any(Wrapper.class))).thenReturn(List.of(stock));

        List<StockAvailabilityDTO> result = service.queryAvailability(
            Arrays.asList(2L, 1L, 2L, null));

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getSkuId());
        assertEquals(0L, result.get(0).getAvailable());
        assertEquals(1L, result.get(1).getSkuId());
        assertEquals(8L, result.get(1).getAvailable());
    }
}
