package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载订单趋势图上的单日点位。
 *   业务边界：date 为日期文本，count 与 amount 分别表示当日单量和已支付金额。
 *   使用场景：OrderTrendVo 聚合后由平台趋势接口返回。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrendPointVo {

    private String date;

    private Long count;

    private BigDecimal amount;
}
