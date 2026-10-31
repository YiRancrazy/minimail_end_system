package com.yirancrazy.minimall.order.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.MerchantRefundExecuteDto;
import com.yirancrazy.minimall.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供商家按退款单号发起、通过和拒绝退款的接口。
 *   业务边界：控制器不直接调用支付服务，退款金额与状态校验由 OrderService 完成。
 *   使用场景：商家售后页面通过 /api/v1/merchant/refunds 调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/refunds")
public class MerchantRefundControllerV1 {

  private final OrderService orderService;

  /**
   * 创建商家退款控制器并注入订单服务。
   *
   * <p>控制器只负责退款单号、身份和请求体绑定。
   *
   * @param orderService 订单业务服务
   */
  public MerchantRefundControllerV1(OrderService orderService) {
    this.orderService = orderService;
  }

  /**
   * 按退款单号发起商家退款。
   *
   * <p>请求体提供金额和原因，商家身份来自请求头。
   *
   * @param refundNo 退款单号，可带 R 前缀
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param dto 商家退款请求体
   * @return 空数据的成功响应。
   */
  @PostMapping("/{refundNo}/execute")
  public Result<Void> executeRefund(
      @PathVariable String refundNo,
      @RequestHeader("X-User-Id") Long merchantId,
      @Valid @RequestBody MerchantRefundExecuteDto dto) {
    orderService.merchantInitiateRefundByNo(
        refundNo, merchantId, dto.getRefundAmount(), dto.getReason());
    return Result.success(null);
  }

  /**
   * 按退款单号通过退款。
   *
   * <p>审核通过后调用支付退款，最终状态由回调推进。
   *
   * @param refundNo 退款单号，可带 R 前缀
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @return 空数据的成功响应。
   */
  @PostMapping("/{refundNo}/approve")
  public Result<Void> approveRefund(
      @PathVariable String refundNo, @RequestHeader("X-Merchant-Id") Long merchantId) {
    orderService.reviewRefundByNo(refundNo, true, merchantId, null);
    return Result.success(null);
  }

  /**
   * 按退款单号驳回退款。
   *
   * <p>驳回原因写入状态日志，订单回退到退款前状态。
   *
   * @param refundNo 退款单号，可带 R 前缀
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param reason 退款、关闭或状态日志说明
   * @return 空数据的成功响应。
   */
  @PostMapping("/{refundNo}/reject")
  public Result<Void> rejectRefund(
      @PathVariable String refundNo,
      @RequestHeader("X-Merchant-Id") Long merchantId,
      @RequestParam String reason) {
    orderService.reviewRefundByNo(refundNo, false, merchantId, reason);
    return Result.success(null);
  }
}
