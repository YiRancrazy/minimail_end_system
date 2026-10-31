package com.yirancrazy.minimall.api.dto.pay;

import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单服务创建支付流水时提交的订单金额、交易双方和支付渠道.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayCreateDto {
  private String orderNo;
  private Long userId;
  private Long merchantId;

  @Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
  private BigDecimal amount;

  private Integer channel;
}
