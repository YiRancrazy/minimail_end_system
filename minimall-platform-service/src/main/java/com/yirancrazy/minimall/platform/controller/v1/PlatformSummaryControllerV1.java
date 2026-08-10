package com.yirancrazy.minimall.platform.controller.v1;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台订单汇总与导出端点（order/pay 聚合摘要）。具体数据由定时任务同步到 Redis；
 *              本端点仅作为占位契约，前端 MSW 兜底 mock 数据；后续接入订单/支付聚合查询。
 * @Version: 1.0
 * @DateTime: 2026/08/09
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/platform")
public class PlatformSummaryControllerV1 {

    /**
     * 订单状态计数摘要。
     */
    @GetMapping("/orders/summary")
    public Result<Map<String, Long>> orderSummary() {
        Map<String, Long> body = new LinkedHashMap<>();
        body.put("paid", 0L);
        body.put("pending", 0L);
        body.put("refunding", 0L);
        body.put("shipped", 0L);
        body.put("completed", 0L);
        body.put("cancelled", 0L);
        return Result.success(body);
    }

    /**
     * 订单导出（CSV）。数据从汇总接口聚合。
     * @param dto 导出范围
     */
    @PostMapping("/orders/export")
    public void exportOrders(@RequestBody ExportDTO dto, HttpServletResponse response) throws IOException {
        log.info("export orders, dto={}", dto);
        CsvExporter.write(response, "orders.csv",
            new String[] { "订单ID", "用户ID", "商家ID", "金额", "状态" },
            List.<String[]>of());
    }

    /**
     * 对账单导出。
     * @param dto 导出范围
     */
    @PostMapping("/pay/reconciliation/export")
    public void exportReconciliation(@RequestBody ExportDTO dto, HttpServletResponse response) throws IOException {
        log.info("export reconciliation, dto={}", dto);
        CsvExporter.write(response, "reconciliation.csv",
            new String[] { "支付单号", "订单号", "金额", "渠道", "状态" },
            List.<String[]>of());
    }

    /** 通用导出入参。 */
    @lombok.Data
    public static class ExportDTO {
        private String startDate;
        private String endDate;
        private String format;
    }

    /**
     * 工具占位：CSV 导出公共方法封装到 CsvExporter 后，此处仅做契约占位。
     */
    static {
        if (CsvExporter.class.getDeclaredMethods().length == 0) {
            throw new BizException("12000", "CsvExporter 未注册");
        }
    }
}