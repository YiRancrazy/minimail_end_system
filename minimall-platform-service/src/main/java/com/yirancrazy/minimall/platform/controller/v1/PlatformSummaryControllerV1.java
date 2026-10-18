package com.yirancrazy.minimall.platform.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.platform.service.PlatformSummaryService;
import com.yirancrazy.minimall.platform.vo.PlatformOrderSummaryVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformSummaryControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformSummaryControllerV1 {

    private static final String[] ORDER_HEADERS = { "订单ID", "用户ID", "商家ID", "金额", "状态" };
    private static final String[] PAY_HEADERS = { "支付单号", "订单号", "金额", "渠道", "状态" };

    private final PlatformSummaryService platformSummaryService;


    /**
     * 执行 orderSummary 方法。
     * @return 统一响应结果
     */
    @GetMapping("/orders/summary")
    public Result<PlatformOrderSummaryVO> orderSummary() {
        return Result.success(platformSummaryService.orderSummary());
    }


    /**
     * 执行 exportOrders 方法。
     * @param dto dto 参数
     * @param response response 参数
     * @throws IOException 处理失败时抛出
     */
    public void exportOrders(@RequestBody ExportDTO dto, HttpServletResponse response) throws IOException {
        List<OrderExportItemDTO> items = platformSummaryService.exportOrders(dto.getStartDate(), dto.getEndDate());
        CsvExporter.write(response, "orders.csv", ORDER_HEADERS, toOrderRows(items));
    }


    /**
     * 执行 exportReconciliation 方法。
     * @param dto dto 参数
     * @param response response 参数
     * @throws IOException 处理失败时抛出
     */
    public void exportReconciliation(@RequestBody ExportDTO dto, HttpServletResponse response) throws IOException {
        List<PayExportItemDTO> items = platformSummaryService.exportReconciliation(
            dto.getStartDate(), dto.getEndDate());
        CsvExporter.write(response, "reconciliation.csv", PAY_HEADERS, toPayRows(items));
    }

    /**
     * 转换或解析订单Rows。
     * @param items 明细列表
     * @return 结果列表
     */
    private List<String[]> toOrderRows(List<OrderExportItemDTO> items) {
        List<String[]> rows = new ArrayList<>(items.size());
        for (OrderExportItemDTO item : items) {
            rows.add(new String[] {
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
     * 转换或解析支付Rows。
     * @param items 明细列表
     * @return 结果列表
     */
    private List<String[]> toPayRows(List<PayExportItemDTO> items) {
        List<String[]> rows = new ArrayList<>(items.size());
        for (PayExportItemDTO item : items) {
            rows.add(new String[] {
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
     * @Description: ExportDTO 数据传输对象。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    @lombok.Data
    public static class ExportDTO {
        private String startDate;
        private String endDate;
        private String format;
    }
}
