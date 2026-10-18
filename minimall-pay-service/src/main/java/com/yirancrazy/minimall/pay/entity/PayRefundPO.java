package com.yirancrazy.minimall.pay.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayRefundPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_pay_refund")
public class PayRefundPO extends BasePO {
    private String refundNo;
    private String refundRequestNo;
    private String paymentNo;
    private String refundTradeNo;
    private BigDecimal amount;
    private String reason;
    private Integer status;
    private LocalDateTime notifiedAt;
    private String idempotencyKey;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
}
