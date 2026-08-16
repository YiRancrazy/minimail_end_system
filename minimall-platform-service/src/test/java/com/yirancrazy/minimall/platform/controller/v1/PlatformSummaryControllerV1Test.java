package com.yirancrazy.minimall.platform.controller.v1;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformSummaryControllerV1 单元测试，验证汇总/导出均委托 Feign 获取真实数据，不再硬编码空值。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
@ExtendWith(MockitoExtension.class)
class PlatformSummaryControllerV1Test {

    @Mock private OrderFeignClient orderFeignClient;
    @Mock private PayFeignClient payFeignClient;

    private PlatformSummaryControllerV1 controller;

    @BeforeEach
    void setUp() {
        controller = new PlatformSummaryControllerV1(orderFeignClient, payFeignClient);
    }

    @Test
    void orderSummary_returns_real_counts_from_order_service() {
        OrderStatisticsDTO s = new OrderStatisticsDTO(100L, new BigDecimal("1000.00"),
            new BigDecimal("10.00"), 20L, 30L, 10L, 15L, 15L, 5L, 5L);
        when(orderFeignClient.statistics()).thenReturn(Result.success(s));

        Result<Map<String, Long>> result = controller.orderSummary();

        assertEquals("00000", result.getCode());
        Map<String, Long> body = result.getData();
        assertEquals(30L, body.get("paid"));
        assertEquals(20L, body.get("pending"));
        assertEquals(5L, body.get("refunding"));
        assertEquals(10L, body.get("shipped"));
        assertEquals(15L, body.get("completed"));
        assertEquals(15L, body.get("cancelled"));
        verify(orderFeignClient).statistics();
    }

    @Test
    void orderSummary_returns_zeros_when_order_service_unavailable() {
        when(orderFeignClient.statistics())
                .thenReturn(Result.fail("20000", "订单服务不可用"));

        Result<Map<String, Long>> result = controller.orderSummary();

        assertEquals("00000", result.getCode());
        Map<String, Long> body = result.getData();
        assertEquals(0L, body.get("paid"));
        assertEquals(0L, body.get("pending"));
        assertEquals(0L, body.get("refunding"));
        assertEquals(0L, body.get("shipped"));
        assertEquals(0L, body.get("completed"));
        assertEquals(0L, body.get("cancelled"));
    }

    @Test
    void exportOrders_writes_csv_from_feign_data() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        OrderExportItemDTO item = new OrderExportItemDTO(1L, 10L, 100L, 5L, 2,
            new BigDecimal("99.00"), 2, LocalDateTime.of(2026, 8, 1, 10, 0));
        when(orderFeignClient.exportList("2026-08-01", "2026-08-16"))
                .thenReturn(Result.success(List.of(item)));

        PlatformSummaryControllerV1.ExportDTO dto = new PlatformSummaryControllerV1.ExportDTO();
        dto.setStartDate("2026-08-01");
        dto.setEndDate("2026-08-16");
        controller.exportOrders(dto, response);

        verify(orderFeignClient).exportList("2026-08-01", "2026-08-16");
        verify(response).setContentType("text/csv; charset=UTF-8");
    }

    @Test
    void exportReconciliation_writes_csv_from_feign_data() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        PayExportItemDTO item = new PayExportItemDTO("P20260801001", "O20260801001",
            10L, 100L, new BigDecimal("99.00"), 1, 2, LocalDateTime.of(2026, 8, 1, 10, 0));
        when(payFeignClient.exportTransactions("2026-08-01", "2026-08-16"))
                .thenReturn(Result.success(List.of(item)));

        PlatformSummaryControllerV1.ExportDTO dto = new PlatformSummaryControllerV1.ExportDTO();
        dto.setStartDate("2026-08-01");
        dto.setEndDate("2026-08-16");
        controller.exportReconciliation(dto, response);

        verify(payFeignClient).exportTransactions("2026-08-01", "2026-08-16");
        verify(response).setContentType("text/csv; charset=UTF-8");
    }
}
