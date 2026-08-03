package com.yirancrazy.minimall.stock.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台库存统计聚合VO，汇总全平台SKU数、可用/预占总量及预警SKU数
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockStatisticsVO {

    /** 有库存记录的SKU总数 */
    private Long totalSkuCount;

    /** 全平台可用库存总和 */
    private Long totalAvailable;

    /** 全平台预占库存总和 */
    private Long totalReserved;

    /** 触发预警的SKU数（available &lt;= alertThreshold） */
    private Long alertSkuCount;

    /** 预警比例，alertSkuCount / totalSkuCount，保留2位小数；无库存时为0 */
    private BigDecimal alertRatio;
}
