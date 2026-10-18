package com.yirancrazy.minimall.order.vo;

import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderStatusCountsVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class OrderStatusCountsVO {

    private long pendingCount;

    private long paidCount;

    private long shippedCount;

    private long completedCount;

    private long canceledCount;

    private long refundingCount;

    private long refundedCount;
}
