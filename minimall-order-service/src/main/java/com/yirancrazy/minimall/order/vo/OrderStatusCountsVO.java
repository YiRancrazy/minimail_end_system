package com.yirancrazy.minimall.order.vo;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户各状态订单数量VO，供"我的"页面订单快捷入口角标展示。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 **/
@Data
public class OrderStatusCountsVO {
    /** 待支付订单数 */
    private long pendingCount;
    /** 待发货订单数 */
    private long paidCount;
    /** 待收货订单数 */
    private long shippedCount;
    /** 已完成订单数 */
    private long completedCount;
}
