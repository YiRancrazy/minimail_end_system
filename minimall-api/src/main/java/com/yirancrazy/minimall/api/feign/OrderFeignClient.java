package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDto;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.api.fallback.OrderFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供支付、统计、评价校验等场景读取和推进订单状态的内部接口.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@FeignClient(name = "minimall-order-service", fallbackFactory = OrderFeignFallbackFactory.class)
public interface OrderFeignClient {
  /**
   * 查询订单当前状态码，供支付等跨服务场景校验状态.
   *
   * @param id 订单 ID
   * @return 包装后的订单状态码
   */
  @GetMapping("/internal/order/{id}")
  Result<Integer> status(@PathVariable("id") Long id);

  /**
   * 按订单 ID 或订单号解析所属商家.
   *
   * @param ref 订单 ID 或订单号
   * @return 包装后的商家 ID
   */
  @GetMapping("/internal/order/merchant/{ref}")
  Result<Long> merchantId(@PathVariable("ref") String ref);

  /**
   * 按订单 ID 或订单号读取支付所需的金额与状态快照.
   *
   * @param ref 订单 ID 或订单号
   * @return 包装后的订单支付快照
   */
  @GetMapping("/internal/order/pay-snapshot/{ref}")
  Result<OrderPaySnapshotDto> paySnapshot(@PathVariable("ref") String ref);

  /**
   * 按订单 ID 将订单推进为已支付状态.
   *
   * @param id 订单 ID
   * @return 状态推进成功或系统错误
   */
  @PostMapping("/internal/order/pay/{id}")
  Result<Void> pay(@PathVariable("id") Long id);

  /**
   * 按订单号将订单推进为已支付状态，用于异步支付回调.
   *
   * @param orderNo 订单号
   * @return 状态推进成功或系统错误
   */
  @PostMapping("/internal/order/pay-by-order-no/{orderNo}")
  Result<Void> payByOrderNo(@PathVariable("orderNo") String orderNo);

  /**
   * 应用支付退款结果并同步订单退款状态.
   *
   * @param dto 本次退款、累计退款及幂等请求号
   * @return 同步成功或系统错误
   */
  @PostMapping("/internal/order/refund-callback")
  Result<Void> refundCallback(@RequestBody RefundCallbackDto dto);

  /**
   * 汇总平台订单数量、金额及各状态订单数.
   *
   * @return 包装后的订单统计
   */
  @GetMapping("/internal/order/statistics")
  Result<OrderStatisticsDto> statistics();

  /**
   * 校验用户是否可对已完成订单中的指定 SPU 发表评价.
   *
   * @param orderNo 订单号
   * @param userId 用户 ID
   * @param spuId 被评价商品 SPU ID
   * @return 包装后的评价资格结果
   */
  @GetMapping("/internal/order/comment-eligibility")
  Result<OrderCommentEligibilityDto> commentEligibility(
      @RequestParam("orderNo") String orderNo,
      @RequestParam("userId") Long userId,
      @RequestParam("spuId") Long spuId);

  /**
   * 按可选日期范围导出订单明细，供对账或报表使用.
   *
   * @param startDate 起始日期，可为空
   * @param endDate 结束日期，可为空
   * @return 包装后的订单导出明细
   */
  @GetMapping("/internal/order/export-list")
  Result<List<OrderExportItemDto>> exportList(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate);
}
