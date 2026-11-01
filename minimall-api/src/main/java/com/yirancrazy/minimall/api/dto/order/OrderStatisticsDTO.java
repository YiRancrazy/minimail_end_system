package com.yirancrazy.minimall.api.dto.order;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台订单仪表盘使用的总量、金额和各状态订单计数.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsDto {

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
