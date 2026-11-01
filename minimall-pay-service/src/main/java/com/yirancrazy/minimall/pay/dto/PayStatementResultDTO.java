package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载支付对账单聚合查询的数据库结果。
 * 包含总笔数与总额、支付、退款和冻结维度的数量及金额，由 Mapper 填充后转换为视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayStatementResultDto {

    private Long totalCount;
    private BigDecimal totalAmount;
    private Long paidCount;
    private BigDecimal paidAmount;
    private Long refundedCount;
    private BigDecimal refundedAmount;
    private Long frozenCount;
    private BigDecimal frozenAmount;
}
