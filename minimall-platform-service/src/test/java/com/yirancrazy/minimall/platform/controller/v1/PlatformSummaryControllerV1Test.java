package com.yirancrazy.minimall.platform.controller.v1;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.service.PlatformSummaryService;
import com.yirancrazy.minimall.platform.vo.PlatformOrderSummaryVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformSummaryControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@ExtendWith(MockitoExtension.class)
class PlatformSummaryControllerV1Test {

    @Mock private PlatformSummaryService platformSummaryService;

    private PlatformSummaryControllerV1 controller;

    @BeforeEach
    void setUp() {
        controller = new PlatformSummaryControllerV1(platformSummaryService);
    }

    @Test
    void orderSummary_delegates_to_service() {
        PlatformOrderSummaryVO summary = new PlatformOrderSummaryVO(
                30L, 20L, 5L, 10L, 15L, 15L);
        when(platformSummaryService.orderSummary()).thenReturn(summary);

        Result<PlatformOrderSummaryVO> result = controller.orderSummary();

        assertEquals("00000", result.getCode());
        PlatformOrderSummaryVO body = result.getData();
        assertEquals(30L, body.getPaid());
        assertEquals(20L, body.getPending());
        assertEquals(5L, body.getRefunding());
        assertEquals(10L, body.getShipped());
        assertEquals(15L, body.getCompleted());
        assertEquals(15L, body.getCancelled());
        verify(platformSummaryService).orderSummary();
    }

    @Test
    void exportOrders_writes_csv_from_service_data() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        when(platformSummaryService.exportOrders("2026-08-01", "2026-08-16"))
                .thenReturn(List.of(new OrderExportItemDTO(
                        1L, 10L, 100L, 5L, 1, new java.math.BigDecimal("99.00"), 2, null)));

        PlatformSummaryControllerV1.ExportDTO dto = new PlatformSummaryControllerV1.ExportDTO();
        dto.setStartDate("2026-08-01");
        dto.setEndDate("2026-08-16");
        controller.exportOrders(dto, response);

        verify(platformSummaryService).exportOrders("2026-08-01", "2026-08-16");
        verify(response).setContentType("text/csv; charset=UTF-8");
    }

    @Test
    void exportReconciliation_writes_csv_from_service_data() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        when(platformSummaryService.exportReconciliation("2026-08-01", "2026-08-16"))
                .thenReturn(List.of(new PayExportItemDTO(
                        "P1", "O1", 10L, 100L, new java.math.BigDecimal("99.00"), 1, 2, null)));

        PlatformSummaryControllerV1.ExportDTO dto = new PlatformSummaryControllerV1.ExportDTO();
        dto.setStartDate("2026-08-01");
        dto.setEndDate("2026-08-16");
        controller.exportReconciliation(dto, response);

        verify(platformSummaryService).exportReconciliation("2026-08-01", "2026-08-16");
        verify(response).setContentType("text/csv; charset=UTF-8");
    }
}
