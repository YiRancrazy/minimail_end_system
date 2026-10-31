package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.pay.PayCreateDto;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDto;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import com.yirancrazy.minimall.api.fallback.PayFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供创建支付流水、发起退款和导出支付流水的内部接口.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@FeignClient(name = "minimall-pay-service", fallbackFactory = PayFeignFallbackFactory.class)
public interface PayFeignClient {
  /**
   * 为待支付订单创建支付流水并返回支付单 ID.
   *
   * @param dto 订单金额、用户、商家和支付渠道
   * @return 包装后的支付单 ID
   */
  @PostMapping("/internal/pay/create")
  Result<Long> create(@RequestBody PayCreateDto dto);

  /**
   * 对指定支付流水发起幂等退款.
   *
   * @param dto 支付单、退款金额、原因和幂等请求号
   * @return 包装后的退款受理结果
   */
  @PostMapping("/internal/pay/refund")
  Result<Boolean> refund(@RequestBody RefundCreateDto dto);

  /**
   * 按可选日期范围导出支付流水，供对账或报表使用.
   *
   * @param startDate 起始日期，可为空
   * @param endDate 结束日期，可为空
   * @return 包装后的支付流水导出明细
   */
  @GetMapping("/internal/pay/export-transactions")
  Result<List<PayExportItemDto>> exportTransactions(
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate);
}
