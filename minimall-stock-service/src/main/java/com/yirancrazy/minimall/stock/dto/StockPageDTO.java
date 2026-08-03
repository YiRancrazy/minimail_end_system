package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存分页查询入参，支持按 SKU 过滤及仅查预警库存
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class StockPageDTO {

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;

    private Long skuId;

    /**
     * 是否仅查询已触发预警的库存（available &lt;= alertThreshold）。
     */
    private Boolean lowStockOnly;
}
