package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单维度统计视图对象，含订单总数、总金额、退款金额及各状态计数。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsVO {
    private Long totalOrderCount;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private Long pendingCount;
    private Long paidCount;
    private Long shippedCount;
    private Long receivedCount;
    private Long cancelledCount;
    private Long refundingCount;
    private Long refundedCount;
}
