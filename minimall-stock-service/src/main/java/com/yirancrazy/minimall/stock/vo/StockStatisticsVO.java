package com.yirancrazy.minimall.stock.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockStatisticsVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockStatisticsVO {


    private Long totalSkuCount;


    private Long totalAvailable;


    private Long totalReserved;


    private Long alertSkuCount;


    private BigDecimal alertRatio;
}
