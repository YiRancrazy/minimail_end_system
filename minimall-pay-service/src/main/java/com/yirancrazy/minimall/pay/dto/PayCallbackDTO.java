package com.yirancrazy.minimall.pay.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 表示支付渠道回调归一化后的内部结果。
 * 承载支付单号、渠道交易号、成功标记、渠道响应和可选回调金额，供支付状态处理使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
public class PayCallbackDto {
  @NotBlank(message = "支付单号不能为空")
  private String paymentNo;

  private String tradeNo;
  private boolean success;
  private String channelResponse;

  @Digits(integer = 10, fraction = 2, message = "回调金额最多10位整数且最多2位小数")
  private BigDecimal totalAmount;

  /**
   * 创建不含回调金额的支付结果对象，兼容旧调用。
   *
   * @param paymentNo 支付单号
   * @param tradeNo 渠道交易号
   * @param success 渠道是否返回支付成功
   * @param channelResponse 渠道原始响应摘要
   */
  public PayCallbackDto(String paymentNo, String tradeNo, boolean success, String channelResponse) {
    this(paymentNo, tradeNo, success, channelResponse, null);
  }

  /**
   * 创建包含可选回调金额的支付结果对象。
   *
   * @param paymentNo 支付单号
   * @param tradeNo 渠道交易号
   * @param success 渠道是否返回支付成功
   * @param channelResponse 渠道原始响应摘要
   * @param totalAmount 渠道回调金额，可为空
   */
  public PayCallbackDto(
      String paymentNo,
      String tradeNo,
      boolean success,
      String channelResponse,
      BigDecimal totalAmount) {
    this.paymentNo = paymentNo;
    this.tradeNo = tradeNo;
    this.success = success;
    this.channelResponse = channelResponse;
    this.totalAmount = totalAmount;
  }
}
