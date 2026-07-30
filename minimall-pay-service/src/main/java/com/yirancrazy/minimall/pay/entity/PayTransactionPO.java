package com.yirancrazy.minimall.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_pay_transaction")
public class PayTransactionPO extends BasePO {
    private String paymentNo;
    private String tradeNo;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private Integer status;
    private Integer channel;
    private String channelResponse;
    private LocalDateTime paidAt;
    private LocalDateTime expireAt;
    private String idempotencyKey;
}