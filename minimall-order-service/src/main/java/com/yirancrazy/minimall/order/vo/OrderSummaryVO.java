package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台或商家订单汇总指标。
 *   业务边界：包含总量、总金额、已支付、待支付、退款及净收入，不包含明细列表。
 *   使用场景：PlatformOrderControllerV1.summary 返回。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryVo {

  private Long totalCount;

  private BigDecimal totalAmount;

  private BigDecimal paidAmount;

  private BigDecimal pendingAmount;

  private BigDecimal refundAmount;

  private BigDecimal netIncome;
}
