package com.yirancrazy.minimall.order.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载订单趋势中的单日单量与金额。
 *   业务边界：date 为按创建时间格式化的日期，count 为当日订单数，amount 为已支付口径金额。
 *   使用场景：OrderMapper.trend 返回后转换为 OrderTrendPointVo。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrendPointDto {

    private String date;
    private Long count;
    private BigDecimal amount;
}
