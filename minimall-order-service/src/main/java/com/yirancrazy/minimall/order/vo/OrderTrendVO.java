package com.yirancrazy.minimall.order.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单趋势视图对象，按日聚合订单数与金额，供平台财务汇总页趋势图消费。
 * @Version: 1.0
 * @DateTime: 2026/08/26
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrendVO {
    /** 按日聚合的数据点列表（按日期升序） */
    private List<OrderTrendPointVO> points;
}
