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
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockJournalVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantStockControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class MerchantStockControllerV1Test {

    private static final long MERCHANT_ID = 1001L;

    private MockMvc mockMvc;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = mock(StockService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantStockControllerV1(stockService)).build();
    }


    @Test
    void get_returns_available_quantity() throws Exception {
        when(stockService.query(99L, MERCHANT_ID)).thenReturn(100L);
        mockMvc.perform(get("/api/v1/merchant/stock/99")
                .header("X-Merchant-Id", MERCHANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(100));
    }


    @Test
    void get_without_merchant_header_returns_400() throws Exception {
        mockMvc.perform(get("/api/v1/merchant/stock/99"))
            .andExpect(status().isBadRequest());
    }


    @Test
    void setThreshold_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/stock/99/threshold")
                .header("X-Merchant-Id", MERCHANT_ID)
                .param("threshold", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(stockService).setThreshold(99L, 50L, MERCHANT_ID);
    }


    @Test
    void adjustStock_passes_quantity_and_reason() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/stock/99/adjust")
                .header("X-Merchant-Id", MERCHANT_ID)
                .param("quantity", "10")
                .param("reason", "补货"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(stockService).adjustStock(99L, 10L, "补货", MERCHANT_ID);
    }


    @Test
    void queryJournal_returns_list() throws Exception {
        StockJournalVO vo = new StockJournalVO();
        vo.setId(1L);
        vo.setSkuId(99L);
        vo.setQuantity(10L);
        vo.setType(1);
        when(stockService.queryJournalVO(99L, MERCHANT_ID)).thenReturn(List.of(vo));
        mockMvc.perform(get("/api/v1/merchant/stock/99/journal")
                .header("X-Merchant-Id", MERCHANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data[0].id").value(1));
    }


    @Test
    void queryJournal_returns_empty_list_when_no_records() throws Exception {
        when(stockService.queryJournalVO(99L, MERCHANT_ID)).thenReturn(Collections.emptyList());
        mockMvc.perform(get("/api/v1/merchant/stock/99/journal")
                .header("X-Merchant-Id", MERCHANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").isArray());
    }


}
