package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 对账单聚合视图对象，含总交易、已支付、已退款、已冻结的笔数与金额
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@NoArgsConstructor
public class PayStatementVO {

    /** 总交易笔数 */
    private Long totalCount;

    /** 总交易金额 */
    private BigDecimal totalAmount;

    /** 已支付笔数（status=2/SUCCESS） */
    private Long paidCount;

    /** 已支付金额 */
    private BigDecimal paidAmount;

    /** 已退款笔数（status=6/REFUNDED） */
    private Long refundedCount;

    /** 已退款金额 */
    private BigDecimal refundedAmount;

    /** 已冻结笔数（status=7/FROZEN） */
    private Long frozenCount;

    /** 已冻结金额 */
    private BigDecimal frozenAmount;
}
