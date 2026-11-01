package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 返回平台支付对账单的聚合视图。
 * 包含总览以及支付、退款、冻结三个维度的笔数与金额，不暴露 Mapper 内部 DTO。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
public class PayStatementVo {

    private Long totalCount;

    private BigDecimal totalAmount;

    private Long paidCount;

    private BigDecimal paidAmount;

    private Long refundedCount;

    private BigDecimal refundedAmount;

    private Long frozenCount;

    private BigDecimal frozenAmount;
}
