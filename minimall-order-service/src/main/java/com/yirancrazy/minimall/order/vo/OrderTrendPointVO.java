package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单趋势单日数据点，供平台财务汇总页折线图渲染（日期/订单数/金额）。
 * @Version: 1.0
 * @DateTime: 2026/08/26
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrendPointVO {
    /** 统计日期（yyyy-MM-dd） */
    private String date;
    /** 当日订单数 */
    private Long count;
    /** 当日已支付订单金额合计 */
    private BigDecimal amount;
}
