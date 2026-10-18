package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayStatementResultDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayStatementResultDTO {

    private Long totalCount;
    private BigDecimal totalAmount;
    private Long paidCount;
    private BigDecimal paidAmount;
    private Long refundedCount;
    private BigDecimal refundedAmount;
    private Long frozenCount;
    private BigDecimal frozenAmount;
}
