package com.yirancrazy.minimall.api.dto.order;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单维度统计跨服务 DTO，供平台端通过 OrderFeignClient 获取全平台订单统计。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsDTO {

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
