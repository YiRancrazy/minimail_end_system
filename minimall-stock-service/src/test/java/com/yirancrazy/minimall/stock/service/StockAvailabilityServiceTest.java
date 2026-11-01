package com.yirancrazy.minimall.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.stock.entity.StockPo;
import com.yirancrazy.minimall.stock.manager.StockCountTaskManager;
import com.yirancrazy.minimall.stock.manager.StockJournalManager;
import com.yirancrazy.minimall.stock.manager.StockManager;
import com.yirancrazy.minimall.stock.manager.StockTransferManager;
import com.yirancrazy.minimall.stock.service.impl.StockServiceImpl;
import java.util.Arrays;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 Mockito 隔离持久层，验证库存可用量批量查询的去重、顺序和默认值规则。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 */
public class StockAvailabilityServiceTest {

    private StockManager stockManager;
    private StockServiceImpl service;

    /**
     * 测试前置：初始化 MyBatis-Plus 表元数据，并构造仅依赖 Mock 管理器的库存服务。
     */
    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.yirancrazy.minimall.stock.mapper.StockMapper");
        TableInfoHelper.initTableInfo(assistant, StockPo.class);

        stockManager = mock(StockManager.class);
        service =
                new StockServiceImpl(
                        stockManager,
                        mock(StockJournalManager.class),
                        mock(StockTransferManager.class),
                        mock(StockCountTaskManager.class));
    }

    /** * 目标验证批量查询去重与默认值；前置请求含重复和空值；步骤调用查询；预期返回顺序 2、1 且缺失量为 0. */
    @Test
    void testQueryAvailabilityReturnsDistinctRequestedSkus() {
        StockPo stock = new StockPo();
        stock.setSkuId(1L);
        stock.setAvailable(8L);
        when(stockManager.list(any(Wrapper.class))).thenReturn(List.of(stock));

        List<StockAvailabilityDto> result = service.queryAvailability(Arrays.asList(2L, 1L, 2L, null));

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getSkuId());
        assertEquals(0L, result.get(0).getAvailable());
        assertEquals(1L, result.get(1).getSkuId());
        assertEquals(8L, result.get(1).getAvailable());
    }
}
