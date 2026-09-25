package com.yirancrazy.minimall.stock.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.stock.service.StockService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalStockControllerV1 MockMvc 单元测试，验证库存调整内部入口。
 * @Version: 1.0
 * @DateTime: 2026/09/25
 */
class InternalStockControllerV1Test {

    private MockMvc mockMvc;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = mock(StockService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
            new InternalStockControllerV1(stockService)).build();
    }

    /**
     * 验证库存调整入口透传 SKU、增量、原因和商家归属。
     */
    @Test
    void adjust_passes_request_to_service() throws Exception {
        mockMvc.perform(post("/internal/stock/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuId\":99,\"merchantId\":1001,\"quantity\":5,"
                    + "\"reason\":\"spu edit\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));

        verify(stockService).adjustStock(99L, 5L, "spu edit", 1001L);
    }

    /**
     * 验证批量预占入口透传 SKU 明细并返回成功。
     */
    @Test
    void reserveBatch_passes_items_to_service() throws Exception {
        mockMvc.perform(post("/internal/stock/reserve-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"skuId\":99,\"quantity\":2},"
                    + "{\"skuId\":100,\"quantity\":3}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));

        verify(stockService).reserveBatch(org.mockito.ArgumentMatchers.argThat(items ->
            items.size() == 2
                && items.get(0).getSkuId() == 99L
                && items.get(0).getQuantity() == 2
                && items.get(1).getSkuId() == 100L
                && items.get(1).getQuantity() == 3));
    }
}
