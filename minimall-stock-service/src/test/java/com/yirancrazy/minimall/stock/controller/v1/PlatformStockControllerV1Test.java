package com.yirancrazy.minimall.stock.controller.v1;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.stock.dto.StockCorrectDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDTO;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferDTO;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;
import com.yirancrazy.minimall.stock.vo.StockVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformStockControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class PlatformStockControllerV1Test {

    private MockMvc mockMvc;
    private StockService stockService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        stockService = mock(StockService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlatformStockControllerV1(stockService)).build();
    }


    @Test
    void page_returns_cursor_result() throws Exception {
        when(stockService.pageVO(any(StockPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/platform/stock/page"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }


    @Test
    void statistics_returns_vo() throws Exception {
        when(stockService.platformStatistics())
            .thenReturn(new StockStatisticsVO(10L, 1000L, 200L, 3L, new BigDecimal("0.30")));
        mockMvc.perform(get("/api/v1/platform/stock/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.totalSkuCount").value(10));
    }


    @Test
    void transfer_with_header_invokes_service() throws Exception {
        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromSkuId(1L);
        dto.setToSkuId(2L);
        dto.setQuantity(10L);
        dto.setReason("调拨");
        mockMvc.perform(post("/api/v1/platform/stock/transfer")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(stockService).transfer(any(StockTransferDTO.class));
    }


    @Test
    void createCountTask_returns_task_id() throws Exception {
        when(stockService.createCountTask(any(StockCountTaskCreateDTO.class))).thenReturn(99L);
        StockCountTaskCreateDTO dto = new StockCountTaskCreateDTO();
        dto.setSkuId(1L);
        dto.setRemark("盘点");
        mockMvc.perform(post("/api/v1/platform/stock/count-tasks")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(99));
    }


    @Test
    void completeCountTask_invokes_service() throws Exception {
        StockCountTaskCompleteDTO dto = new StockCountTaskCompleteDTO();
        dto.setActualQuantity(100L);
        dto.setRemark("盘平");
        mockMvc.perform(post("/api/v1/platform/stock/count-tasks/99/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(stockService).completeCountTask(any(Long.class), any(StockCountTaskCompleteDTO.class));
    }


    @Test
    void correct_with_header_invokes_service() throws Exception {
        StockCorrectDTO dto = new StockCorrectDTO();
        dto.setCorrectQuantity(100L);
        dto.setReason("纠正");
        mockMvc.perform(post("/api/v1/platform/stock/99/correct")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(stockService).correctStock(any(Long.class), any(Long.class), any());
    }


    @Test
    void listAbnormal_returns_list() throws Exception {
        StockVO vo = new StockVO();
        vo.setId(1L);
        vo.setSkuId(99L);
        vo.setAvailable(-5L);
        when(stockService.listAbnormalStockVO()).thenReturn(java.util.List.of(vo));
        mockMvc.perform(get("/api/v1/platform/stock/abnormal"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data[0].id").value(1));
    }
}
