package com.yirancrazy.minimall.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 `t_pay_refund` 退款流水表。
 * 记录支付单退款金额、状态、请求幂等键、通知时间和重试计划，供退款编排及补偿扫描使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_pay_refund")
public class PayRefundPo extends BasePo {
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
