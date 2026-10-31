package com.yirancrazy.minimall.api.dto.order;

import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付前读取的订单金额、状态及支付单关联快照.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaySnapshotDto {

  private Long orderId;
  private String orderNo;
  private Long userId;
  private Long merchantId;

  @Digits(integer = 10, fraction = 2, message = "payAmount must have at most 2 decimal places")
  private BigDecimal payAmount;

  private Integer status;
  private Long payId;
}
