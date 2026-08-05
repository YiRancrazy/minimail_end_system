package com.yirancrazy.minimall.stock.controller.v1;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.service.StockService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantStockControllerV1 MockMvc 单元测试，验证 HTTP 层路由、参数绑定与 Result 包装。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class MerchantStockControllerV1Test {

    private MockMvc mockMvc;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = mock(StockService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantStockControllerV1(stockService)).build();
    }

    /**
     * 验证 GET /api/v1/merchant/stock/{skuId} 返回 SKU 可用库存数量。
     */
    @Test
    void get_returns_available_quantity() throws Exception {
        when(stockService.query(99L)).thenReturn(100L);
        mockMvc.perform(get("/api/v1/merchant/stock/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(100));
    }

    /**
     * 验证 POST /api/v1/merchant/stock/{skuId}/threshold 调用 service 设置预警阈值。
     */
    @Test
    void setThreshold_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/stock/99/threshold")
                .param("threshold", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(stockService).setThreshold(99L, 50L);
    }

    /**
     * 验证 POST /api/v1/merchant/stock/{skuId}/adjust 传递 quantity 与 reason 至 service。
     */
    @Test
    void adjustStock_passes_quantity_and_reason() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/stock/99/adjust")
                .param("quantity", "10")
                .param("reason", "补货"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(stockService).adjustStock(99L, 10L, "补货");
    }

    /**
     * 验证 GET /api/v1/merchant/stock/{skuId}/journal 返回库存流水列表。
     */
    @Test
    void queryJournal_returns_list() throws Exception {
        StockJournalPO po = new StockJournalPO();
        po.setId(1L);
        po.setSkuId(99L);
        po.setQuantity(10L);
        po.setType(1);
        when(stockService.queryJournal(99L)).thenReturn(List.of(po));
        mockMvc.perform(get("/api/v1/merchant/stock/99/journal"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data[0].id").value(1));
    }

    /**
     * 验证 GET /api/v1/merchant/stock/{skuId}/journal 在无流水时返回空列表。
     */
    @Test
    void queryJournal_returns_empty_list_when_no_records() throws Exception {
        when(stockService.queryJournal(99L)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/merchant/stock/99/journal"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").isArray());
    }
}
