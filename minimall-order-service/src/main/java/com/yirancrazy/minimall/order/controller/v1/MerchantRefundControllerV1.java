package com.yirancrazy.minimall.order.controller.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.MerchantRefundExecuteDTO;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端退款控制器，路径独立于订单控制器（/api/v1/merchant/refunds），承载退款发起、审核、驳回能力。
 * @Version: 1.0
 * @DateTime: 2026/08/22
 */
@RestController
@RequestMapping("/api/v1/merchant/refunds")
public class MerchantRefundControllerV1 {

    private final OrderService orderService;

    public MerchantRefundControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 商家主动发起退款。退款单号由调用方生成（业务幂等键），此处直接复用 orderId 形式。
     * @param refundNo 退款单号
     * @param merchantId 商家ID
     * @param dto 退款入参（金额 + 原因）
     */
    @PostMapping("/{refundNo}/execute")
    public Result<Void> executeRefund(@PathVariable String refundNo,
                                       @RequestHeader("X-User-Id") Long merchantId,
                                       @Valid @RequestBody MerchantRefundExecuteDTO dto) {
        orderService.merchantInitiateRefund(parseOrderIdFromRefundNo(refundNo), merchantId,
            dto.getRefundAmount(), dto.getReason());
        return Result.success(null);
    }

    /**
     * 商家同意退款（以退款单号为幂等键）。退款单号约定以 R 开头后跟订单ID。
     * @param refundNo 退款单号
     * @param merchantId 商家ID
     */
    @PostMapping("/{refundNo}/approve")
    public Result<Void> approveRefund(@PathVariable String refundNo,
                                      @RequestHeader("X-Merchant-Id") Long merchantId) {
        orderService.reviewRefund(parseOrderIdFromRefundNo(refundNo), true, merchantId, null);
        return Result.success(null);
    }

    /**
     * 商家驳回退款（以退款单号为幂等键），驳回原因写入状态日志供回显。
     * @param refundNo 退款单号
     * @param merchantId 商家ID
     * @param reason 驳回原因
     */
    @PostMapping("/{refundNo}/reject")
    public Result<Void> rejectRefund(@PathVariable String refundNo,
                                     @RequestHeader("X-Merchant-Id") Long merchantId,
                                     @RequestParam String reason) {
        orderService.reviewRefund(parseOrderIdFromRefundNo(refundNo), false, merchantId, reason);
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
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("refundNo 格式错误：" + refundNo);
        }
    }
}
