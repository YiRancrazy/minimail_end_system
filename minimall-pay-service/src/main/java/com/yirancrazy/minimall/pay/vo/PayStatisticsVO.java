package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 返回商家或平台支付统计视图。
 * 包含成功交易总额、退款总额和交易笔数，供管理端汇总展示。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayStatisticsVo {
  private BigDecimal totalAmount;
  private BigDecimal refundAmount;
  private Long transactionCount;
}
