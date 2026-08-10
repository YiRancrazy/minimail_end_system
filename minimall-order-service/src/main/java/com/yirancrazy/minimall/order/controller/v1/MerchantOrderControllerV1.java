package com.yirancrazy.minimall.order.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.order.dto.MerchantRefundExecuteDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.dto.OrderShipDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端订单控制器，提供商家视角的订单 RESTful API
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@RestController
@RequestMapping("/api/v1/merchant/orders")
public class MerchantOrderControllerV1 {

    private static final String[] ORDER_HEADERS = {
        "订单ID", "用户ID", "商家ID", "SKU", "数量", "金额", "状态", "创建时间"
    };

    private final OrderService orderService;

    public MerchantOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 商家端游标分页查询订单列表，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 游标分页查询入参
     * @return 订单游标分页结果
     */
    @GetMapping
    public Result<CursorPageVO<OrderPO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                               @Valid OrderPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(orderService.page(dto));
    }

    /**
     * 发货。
     * @param orderId    订单ID
     * @param merchantId 商户ID
     */
    @PostMapping("/{orderId}/ship")
    public Result<Void> ship(@PathVariable Long orderId,
                             @RequestHeader("X-User-Id") Long merchantId,
                             @Valid @RequestBody OrderShipDTO dto) {
        orderService.ship(orderId, merchantId, dto.getCarrier(), dto.getTrackingNo());
        return Result.success(null);
    }

    /**
     * 商家主动发起退款。退款单号由调用方生成（业务幂等键），此处直接复用 orderId 形式。
     * @param refundNo 退款单号
     * @param merchantId 商家ID
     * @param dto 退款入参（金额 + 原因）
     */
    @PostMapping("/refunds/{refundNo}/execute")
    public Result<Void> executeRefund(@PathVariable String refundNo,
                                       @RequestHeader("X-User-Id") Long merchantId,
                                       @Valid @RequestBody MerchantRefundExecuteDTO dto) {
        Long orderId = parseOrderIdFromRefundNo(refundNo);
        orderService.merchantInitiateRefund(orderId, merchantId, dto.getRefundAmount(), dto.getReason());
        return Result.success(null);
    }

    private Long parseOrderIdFromRefundNo(String refundNo) {
        if (refundNo == null || refundNo.isEmpty()) {
            throw new IllegalArgumentException("refundNo 不能为空");
        }
        // 约定：refundNo 以 R 开头，后跟订单ID
        String tail = refundNo.startsWith("R") ? refundNo.substring(1) : refundNo;
        try {
            return Long.parseLong(tail);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("refundNo 格式错误：" + refundNo);
        }
    }

    /**
     * 商家关闭订单，仅允许待支付订单关闭并释放库存。
     * @param orderId 订单ID
     * @param merchantId 商家ID
     */
    @PostMapping("/{orderId}/close")
    public Result<Void> merchantClose(@PathVariable Long orderId,
                                      @RequestHeader("X-Merchant-Id") Long merchantId) {
        orderService.merchantClose(orderId, merchantId);
        return Result.success(null);
    }

    /**
     * 商家待处理订单数量统计，包含 PENDING/PAID/REFUNDING 三种状态。
     * @param merchantId 商家ID
     * @return 待处理订单总数
     */
    @GetMapping("/_count")
    public Result<Long> pendingCount(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(orderService.pendingCount(merchantId));
    }

    /**
     * 商家审核退款，merchantId 由可信 Header 注入。
     * @param orderId 订单ID
     * @param merchantId 商家ID
     * @param approved 是否同意退款
     */
    @PostMapping("/{orderId}/refund-review")
    public Result<Void> refundReview(@PathVariable Long orderId,
                                     @RequestHeader("X-Merchant-Id") Long merchantId,
                                     @RequestParam boolean approved) {
        orderService.reviewRefund(orderId, approved, merchantId);
        return Result.success(null);
    }

    /**
     * 商家导出订单 CSV，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 查询入参
     * @param response HTTP 响应
     * @throws IOException 写入失败时抛出
     */
    @GetMapping("/export")
    public void export(@RequestHeader("X-Merchant-Id") Long merchantId,
                       @Valid OrderPageDTO dto,
                       HttpServletResponse response) throws IOException {
        List<OrderPO> list = orderService.exportList(merchantId, dto);
        CsvExporter.write(response, "orders.csv", ORDER_HEADERS, toOrderRows(list));
    }

    private List<String[]> toOrderRows(List<OrderPO> list) {
        List<String[]> rows = new ArrayList<>(list.size());
        for (OrderPO po : list) {
            rows.add(new String[] {
                String.valueOf(po.getId()),
                String.valueOf(po.getUserId()),
                String.valueOf(po.getMerchantId()),
                String.valueOf(po.getSkuId()),
                String.valueOf(po.getQuantity()),
                po.getAmount() == null ? "" : po.getAmount().toPlainString(),
                String.valueOf(po.getStatus()),
                po.getCreateTime() == null ? "" : po.getCreateTime().toString()
            });
        }
        return rows;
    }
}
