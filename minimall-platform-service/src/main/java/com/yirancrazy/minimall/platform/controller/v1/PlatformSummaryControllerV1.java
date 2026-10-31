package com.yirancrazy.minimall.platform.controller.v1;

import com.yirancrazy.minimall.api.dto.order.OrderExportItemDto;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.platform.service.PlatformSummaryService;
import com.yirancrazy.minimall.platform.vo.PlatformOrderSummaryVo;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台经营汇总接口，提供订单状态统计及订单、支付对账 CSV 导出.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformSummaryControllerV1 {

  private static final String[] ORDER_HEADERS = {"订单ID", "用户ID", "商家ID", "金额", "状态"};
  private static final String[] PAY_HEADERS = {"支付单号", "订单号", "金额", "渠道", "状态"};

  private final PlatformSummaryService platformSummaryService;

  /**
   * 查询平台订单各业务状态的汇总数量.
   *
   * @return 订单状态数量汇总
   */
  @GetMapping("/orders/summary")
  public Result<PlatformOrderSummaryVo> orderSummary() {
    return Result.success(platformSummaryService.orderSummary());
  }

  /**
   * 按日期范围查询订单导出数据，并将订单 CSV 写入响应流.
   *
   * @param dto 导出日期范围
   * @param response HTTP 响应，用于承载 orders.csv 附件内容
   * @throws IOException 写入响应流失败时抛出
   */
  public void exportOrders(@RequestBody ExportDto dto, HttpServletResponse response)
      throws IOException {
    List<OrderExportItemDto> items =
        platformSummaryService.exportOrders(dto.getStartDate(), dto.getEndDate());
    CsvExporter.write(response, "orders.csv", ORDER_HEADERS, toOrderRows(items));
  }

  /**
   * 按日期范围查询支付对账数据，并将对账 CSV 写入响应流.
   *
   * @param dto 导出日期范围
   * @param response HTTP 响应，用于承载 reconciliation.csv 附件内容
   * @throws IOException 写入响应流失败时抛出
   */
  public void exportReconciliation(@RequestBody ExportDto dto, HttpServletResponse response)
      throws IOException {
    List<PayExportItemDto> items =
        platformSummaryService.exportReconciliation(dto.getStartDate(), dto.getEndDate());
    CsvExporter.write(response, "reconciliation.csv", PAY_HEADERS, toPayRows(items));
  }

  /**
   * 将订单导出明细转换为 CSV 数据行，保持输入顺序，空金额写为空字段.
   *
   * @param items 订单导出明细
   * @return 与订单明细一一对应的 CSV 行
   */
  private List<String[]> toOrderRows(List<OrderExportItemDto> items) {
    List<String[]> rows = new ArrayList<>(items.size());
    for (OrderExportItemDto item : items) {
      rows.add(
          new String[] {
            String.valueOf(item.getId()),
            String.valueOf(item.getUserId()),
            String.valueOf(item.getMerchantId()),
            item.getAmount() == null ? "" : item.getAmount().toPlainString(),
            String.valueOf(item.getStatus())
          });
    }
    return rows;
  }

  /**
   * 将支付对账明细转换为 CSV 数据行，保持输入顺序，空金额写为空字段.
   *
   * @param items 支付对账明细
   * @return 与支付明细一一对应的 CSV 行
   */
  private List<String[]> toPayRows(List<PayExportItemDto> items) {
    List<String[]> rows = new ArrayList<>(items.size());
    for (PayExportItemDto item : items) {
      rows.add(
          new String[] {
            item.getPaymentNo(),
            item.getOrderNo(),
            item.getAmount() == null ? "" : item.getAmount().toPlainString(),
            String.valueOf(item.getChannel()),
            String.valueOf(item.getStatus())
          });
    }
    return rows;
  }

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 平台订单及支付对账导出的日期范围请求体.
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  @lombok.Data
  public static class ExportDto {
    private String startDate;
    private String endDate;
    private String format;
  }
}
