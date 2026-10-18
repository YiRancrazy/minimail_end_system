package com.yirancrazy.minimall.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockStatisticsDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockStatisticsDTO {

    private Long totalSkuCount;
    private Long totalAvailable;
    private Long totalReserved;
    private Long alertSkuCount;
}
