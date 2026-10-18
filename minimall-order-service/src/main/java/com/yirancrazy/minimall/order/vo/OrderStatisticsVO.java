package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderStatisticsVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
