package com.yirancrazy.minimall.order.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载订单汇总查询的总量、已支付、待支付、退款和净收入指标。
 *   业务边界：字段来自 OrderMapper SQL 投影，金额口径为已支付状态订单及退款状态订单。
 *   使用场景：平台或商家订单汇总接口使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResultDto {

    private Long totalCount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private BigDecimal refundAmount;
    private BigDecimal netIncome;
}
