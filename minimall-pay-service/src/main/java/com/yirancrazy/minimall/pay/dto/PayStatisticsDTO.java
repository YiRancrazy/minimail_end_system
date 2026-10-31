package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载商家或平台支付统计的数据库聚合结果。
 * 包含成功交易总额、退款总额和交易笔数，由 `PayManager` 转换为接口视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayStatisticsDto {

  private BigDecimal totalAmount;
  private BigDecimal refundAmount;
  private Long transactionCount;
}
