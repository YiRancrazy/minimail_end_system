package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台订单统计接口的总量和分状态计数。
 *   业务边界：字段由 OrderStatisticsResultDto 转换，金额口径沿用 Mapper 的已支付与退款状态定义。
 *   使用场景：PlatformOrderControllerV1.statistics 返回。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsVo {
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
