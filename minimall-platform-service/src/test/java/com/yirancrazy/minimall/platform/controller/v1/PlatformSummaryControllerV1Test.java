package com.yirancrazy.minimall.platform.controller.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.order.OrderExportItemDto;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.service.PlatformSummaryService;
import com.yirancrazy.minimall.platform.vo.PlatformOrderSummaryVo;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台经营汇总控制器的订单统计与 CSV 导出委派单元测试.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
class PlatformSummaryControllerV1Test {

  @Mock private PlatformSummaryService platformSummaryService;

  private PlatformSummaryControllerV1 controller;

  /** 使用 Mockito 汇总服务替身为每个测试创建控制器实例。 */
  @BeforeEach
  void setUp() {
    controller = new PlatformSummaryControllerV1(platformSummaryService);
  }

  /** 目标：验证订单汇总映射；前置：服务返回六种非零计数；步骤：调用 orderSummary；预期：code=00000 且六个计数逐项一致. */
  @Test
  void testOrderSummaryDelegatesToService() {
    PlatformOrderSummaryVo summary = new PlatformOrderSummaryVo(30L, 20L, 5L, 10L, 15L, 15L);
    when(platformSummaryService.orderSummary()).thenReturn(summary);

    Result<PlatformOrderSummaryVo> result = controller.orderSummary();

    assertEquals("00000", result.getCode());
    PlatformOrderSummaryVo body = result.getData();
    assertEquals(30L, body.getPaid());
    assertEquals(20L, body.getPending());
    assertEquals(5L, body.getRefunding());
    assertEquals(10L, body.getShipped());
    assertEquals(15L, body.getCompleted());
    assertEquals(15L, body.getCancelled());
    verify(platformSummaryService).orderSummary();
  }

  /** 目标：验证订单 CSV 导出；前置：输出流可写且服务返回一条明细；步骤：导出 2026-08-01 至 2026-08-16；预期：日期一致且响应为 UTF-8 CSV. */
  @Test
  void testExportOrdersWritesCsvFromServiceData() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
    when(platformSummaryService.exportOrders("2026-08-01", "2026-08-16"))
        .thenReturn(
            List.of(
                new OrderExportItemDto(
                    1L, 10L, 100L, 5L, 1, new java.math.BigDecimal("99.00"), 2, null)));

    PlatformSummaryControllerV1.ExportDto dto = new PlatformSummaryControllerV1.ExportDto();
    dto.setStartDate("2026-08-01");
    dto.setEndDate("2026-08-16");
    controller.exportOrders(dto, response);

    verify(platformSummaryService).exportOrders("2026-08-01", "2026-08-16");
    verify(response).setContentType("text/csv; charset=UTF-8");
  }

  /** 目标：验证支付对账 CSV 导出；前置：输出流可写且服务返回一条流水；步骤：导出指定日期范围；预期：日期一致且响应为 UTF-8 CSV. */
  @Test
  void testExportReconciliationWritesCsvFromServiceData() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
    when(platformSummaryService.exportReconciliation("2026-08-01", "2026-08-16"))
        .thenReturn(
            List.of(
                new PayExportItemDto(
                    "P1", "O1", 10L, 100L, new java.math.BigDecimal("99.00"), 1, 2, null)));

    PlatformSummaryControllerV1.ExportDto dto = new PlatformSummaryControllerV1.ExportDto();
    dto.setStartDate("2026-08-01");
    dto.setEndDate("2026-08-16");
    controller.exportReconciliation(dto, response);

    verify(platformSummaryService).exportReconciliation("2026-08-01", "2026-08-16");
    verify(response).setContentType("text/csv; charset=UTF-8");
  }
}
