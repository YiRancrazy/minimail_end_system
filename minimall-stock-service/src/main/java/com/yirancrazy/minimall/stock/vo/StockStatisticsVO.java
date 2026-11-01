package com.yirancrazy.minimall.stock.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 向平台接口输出库存总量、告警 SKU 数及计算后的告警占比。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockStatisticsVo {

    private Long totalSkuCount;

    private Long totalAvailable;

    private Long totalReserved;

    private Long alertSkuCount;

    private BigDecimal alertRatio;
}
