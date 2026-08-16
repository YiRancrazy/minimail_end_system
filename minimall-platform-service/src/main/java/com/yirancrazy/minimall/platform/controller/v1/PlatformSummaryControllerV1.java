package com.yirancrazy.minimall.platform.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台订单汇总与导出端点（order/pay 聚合摘要），经 Feign 委托 order/pay 服务真实聚合；
 *              服务不可用时由 Feign fallback 降级为空数据并记录告警。
 * @Version: 2.0
 * @DateTime: 2026/08/16
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformSummaryControllerV1 {

    private static final String[] ORDER_HEADERS = { "订单ID", "用户ID", "商家ID", "金额", "状态" };
    private static final String[] PAY_HEADERS = { "支付单号", "订单号", "金额", "渠道", "状态" };

    private final OrderFeignClient orderFeignClient;
    private final PayFeignClient payFeignClient;

    /**
     * 订单状态计数摘要，按订单服务统计口径聚合：completed 对应用户端已收货（COMPLETED）状态。
     * @return 各状态订单计数
     */
    @GetMapping("/orders/summary")
    public Result<Map<String, Long>> orderSummary() {
        Result<OrderStatisticsDTO> r = orderFeignClient.statistics();
        if (r == null || !CommonCode.SUCCESS.equals(r.getCode()) || r.getData() == null) {
            log.warn("order statistics unavailable, summary returns zeros. result={}", r);
            return Result.success(zeros());
        }
        OrderStatisticsDTO s = r.getData();
        Map<String, Long> body = new LinkedHashMap<>();
        body.put("paid", nvl(s.getPaidCount()));
        body.put("pending", nvl(s.getPendingCount()));
        body.put("refunding", nvl(s.getRefundingCount()));
        body.put("shipped", nvl(s.getShippedCount()));
        body.put("completed", nvl(s.getReceivedCount()));
        body.put("cancelled", nvl(s.getCancelledCount()));
        return Result.success(body);
    }

    /**
     * 订单导出（CSV），基于订单服务真实查询结果。
     * @param dto 导出范围（startDate/endDate 为 yyyy-MM-dd）
     * @param response HTTP 响应
     * @throws IOException 写入失败时抛出
     */
    @PostMapping("/orders/export")
    public void exportOrders(@RequestBody ExportDTO dto, HttpServletResponse response) throws IOException {
        Result<List<OrderExportItemDTO>> r =
                orderFeignClient.exportList(dto.getStartDate(), dto.getEndDate());
        if (r == null || !CommonCode.SUCCESS.equals(r.getCode()) || r.getData() == null) {
            log.warn("order export data unavailable, export empty file. result={}", r);
            CsvExporter.write(response, "orders.csv", ORDER_HEADERS, List.of());
            return;
        }
        List<String[]> rows = new ArrayList<>(r.getData().size());
        for (OrderExportItemDTO item : r.getData()) {
            rows.add(new String[] {
                String.valueOf(item.getId()),
                String.valueOf(item.getUserId()),
                String.valueOf(item.getMerchantId()),
                item.getAmount() == null ? "" : item.getAmount().toPlainString(),
                String.valueOf(item.getStatus())
            });
        }
        CsvExporter.write(response, "orders.csv", ORDER_HEADERS, rows);
    }

    /**
     * 对账单导出，基于支付服务真实流水。
     * @param dto 导出范围（startDate/endDate 为 yyyy-MM-dd）
     * @param response HTTP 响应
     * @throws IOException 写入失败时抛出
     */
    @PostMapping("/pay/reconciliation/export")
    public void exportReconciliation(@RequestBody ExportDTO dto, HttpServletResponse response) throws IOException {
        Result<List<PayExportItemDTO>> r =
                payFeignClient.exportTransactions(dto.getStartDate(), dto.getEndDate());
        if (r == null || !CommonCode.SUCCESS.equals(r.getCode()) || r.getData() == null) {
            log.warn("pay reconciliation data unavailable, export empty file. result={}", r);
            CsvExporter.write(response, "reconciliation.csv", PAY_HEADERS, List.of());
            return;
        }
        List<String[]> rows = new ArrayList<>(r.getData().size());
        for (PayExportItemDTO item : r.getData()) {
            rows.add(new String[] {
                item.getPaymentNo(),
                item.getOrderNo(),
                item.getAmount() == null ? "" : item.getAmount().toPlainString(),
                String.valueOf(item.getChannel()),
                String.valueOf(item.getStatus())
            });
        }
        CsvExporter.write(response, "reconciliation.csv", PAY_HEADERS, rows);
    }

    /**
     * 全零统计兜底，仅在订单服务不可达时返回。
     * @return 各状态计数均为 0 的映射
     */
    private Map<String, Long> zeros() {
        Map<String, Long> body = new LinkedHashMap<>();
        body.put("paid", 0L);
        body.put("pending", 0L);
        body.put("refunding", 0L);
        body.put("shipped", 0L);
        body.put("completed", 0L);
        body.put("cancelled", 0L);
        return body;
    }

    /**
     * 空值保护。
     * @param v 统计值
     * @return 非空统计值，null 返回 0
     */
    private long nvl(Long v) {
        return v == null ? 0L : v;
    }

    /** 通用导出入参。 */
    @lombok.Data
    public static class ExportDTO {
        private String startDate;
        private String endDate;
        private String format;
    }
}
