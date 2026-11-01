package com.yirancrazy.minimall.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承接库存聚合查询返回的 SKU 数、可用量、预占量和告警数。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockStatisticsDto {

    private Long totalSkuCount;
    private Long totalAvailable;
    private Long totalReserved;
    private Long alertSkuCount;
}
