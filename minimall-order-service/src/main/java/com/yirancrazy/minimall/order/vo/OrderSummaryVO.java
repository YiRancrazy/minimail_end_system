package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderSummaryVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryVO {

    private Long totalCount;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private BigDecimal pendingAmount;

    private BigDecimal refundAmount;

    private BigDecimal netIncome;
}
