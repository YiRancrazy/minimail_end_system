package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台订单财务汇总视图对象，供平台端财务汇总页展示区间订单金额概览。
 * @Version: 1.0
 * @DateTime: 2026/08/26
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryVO {
    /** 区间订单总数 */
    private Long totalCount;
    /** GMV：已支付（PAID/SHIPPED/COMPLETED）订单金额合计 */
    private BigDecimal totalAmount;
    /** 已支付金额（口径同 GMV） */
    private BigDecimal paidAmount;
    /** 待支付金额 */
    private BigDecimal pendingAmount;
    /** 退款金额：退款中 + 已退款订单金额合计 */
    private BigDecimal refundAmount;
    /** 净收入：GMV - 退款金额 */
    private BigDecimal netIncome;
}
