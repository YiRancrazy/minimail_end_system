package com.yirancrazy.minimall.pay.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 `t_pay_transaction` 支付交易表。
 * 保存订单、用户、商家、金额及支付状态；渠道响应通过类型处理器加密落库。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_pay_transaction", autoResultMap = true)
public class PayTransactionPo extends BasePo {
  private String paymentNo;
  private String tradeNo;
  private String orderNo;
  private String activeOrderNo;
  private Long userId;
  private Long merchantId;
  private BigDecimal amount;
  private String currency;
  private Integer status;
  private Integer channel;

  @TableField(typeHandler = EncryptedStringTypeHandler.class)
  private String channelResponse;

  private LocalDateTime paidAt;
  private LocalDateTime expireAt;
  private String idempotencyKey;
}
