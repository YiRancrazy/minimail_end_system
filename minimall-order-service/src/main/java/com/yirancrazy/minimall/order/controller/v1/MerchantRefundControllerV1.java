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
 * @Description: MerchantRefundControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/refunds")
public class MerchantRefundControllerV1 {

    private final OrderService orderService;

    /**
     * 构造 MerchantRefundControllerV1 实例。
     * @param orderService orderService 参数
     */
    public MerchantRefundControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }


    /**
     * 执行 executeRefund 方法。
     * @param refundNo refundNo 参数
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/{refundNo}/execute")
    public Result<Void> executeRefund(@PathVariable String refundNo,
                                       @RequestHeader("X-User-Id") Long merchantId,
                                       @Valid @RequestBody MerchantRefundExecuteDTO dto) {
        orderService.merchantInitiateRefundByNo(refundNo, merchantId,
            dto.getRefundAmount(), dto.getReason());
        return Result.success(null);
    }


    /**
     * 执行 approveRefund 方法。
     * @param refundNo refundNo 参数
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @PostMapping("/{refundNo}/approve")
    public Result<Void> approveRefund(@PathVariable String refundNo,
                                      @RequestHeader("X-Merchant-Id") Long merchantId) {
        orderService.reviewRefundByNo(refundNo, true, merchantId, null);
        return Result.success(null);
    }


    /**
     * 执行 rejectRefund 方法。
     * @param refundNo refundNo 参数
     * @param merchantId merchantId 参数
     * @param reason reason 参数
     * @return 统一响应结果
     */
    @PostMapping("/{refundNo}/reject")
    public Result<Void> rejectRefund(@PathVariable String refundNo,
                                     @RequestHeader("X-Merchant-Id") Long merchantId,
                                     @RequestParam String reason) {
        orderService.reviewRefundByNo(refundNo, false, merchantId, reason);
        return Result.success(null);
    }
}
