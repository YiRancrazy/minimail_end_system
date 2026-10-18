package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayStatementVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
public class PayStatementVO {


    private Long totalCount;


    private BigDecimal totalAmount;


    private Long paidCount;


    private BigDecimal paidAmount;


    private Long refundedCount;


    private BigDecimal refundedAmount;


    private Long frozenCount;


    private BigDecimal frozenAmount;
}
