package com.yirancrazy.minimall.order.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载按日期聚合的订单趋势序列。
 *   业务边界：只保存点位列表；无趋势数据时使用空列表而不是 null。
 *   使用场景：PlatformOrderControllerV1.trend 返回。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrendVo {

    private List<OrderTrendPointVo> points;
}
