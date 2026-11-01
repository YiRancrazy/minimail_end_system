package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 返回退款处理结果的精简视图。
 * 包含退款单号、支付单号、退款金额和状态，供内部退款调用方确认结果。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundVo {
    private String refundNo;
    private String paymentNo;
    private BigDecimal amount;
    private Integer status;
}
