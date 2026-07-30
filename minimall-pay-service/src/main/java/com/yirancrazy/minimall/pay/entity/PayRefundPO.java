package com.yirancrazy.minimall.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_pay_refund")
public class PayRefundPO extends BasePO {
    private String refundNo;
    private String paymentNo;
    private String refundTradeNo;
    private BigDecimal amount;
    private String reason;
    private Integer status;
    private LocalDateTime notifiedAt;
    private String idempotencyKey;
}