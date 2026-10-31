package com.yirancrazy.minimall.api.dto.pay;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 对已支付流水发起幂等退款的请求参数.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
public class RefundCreateDto {
  @NotNull(message = "payId cannot be null")
  private Long payId;

  @NotNull(message = "amount cannot be null")
  @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
  @Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
  private BigDecimal amount;

  private String reason;

  @NotBlank(message = "refundRequestNo cannot be blank")
  private String refundRequestNo;

  /**
   * 创建不带退款请求号的退款参数.
   *
   * @param payId 支付流水 ID
   * @param amount 本次退款金额
   * @param reason 退款原因
   */
  public RefundCreateDto(Long payId, BigDecimal amount, String reason) {
    this(payId, amount, reason, null);
  }

  /**
   * 创建包含幂等退款请求号的完整退款参数.
   *
   * @param payId 支付流水 ID
   * @param amount 本次退款金额
   * @param reason 退款原因
   * @param refundRequestNo 防止重复退款的幂等请求号
   */
  public RefundCreateDto(Long payId, BigDecimal amount, String reason, String refundRequestNo) {
    this.payId = payId;
    this.amount = amount;
    this.reason = reason;
    this.refundRequestNo = refundRequestNo;
  }
}
