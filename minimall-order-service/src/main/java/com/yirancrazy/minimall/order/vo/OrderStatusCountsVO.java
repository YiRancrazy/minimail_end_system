package com.yirancrazy.minimall.order.vo;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载单个用户各订单状态的数量汇总。
 *   业务边界：缺失状态默认保持 0，供用户订单中心显示角标和筛选数量。
 *   使用场景：UserOrderControllerV1.statusCounts 返回。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class OrderStatusCountsVo {

    private long pendingCount;

    private long paidCount;

    private long shippedCount;

    private long completedCount;

    private long canceledCount;

    private long refundingCount;

    private long refundedCount;
}
