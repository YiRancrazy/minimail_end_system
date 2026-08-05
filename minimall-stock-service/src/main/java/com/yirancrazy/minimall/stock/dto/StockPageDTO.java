package com.yirancrazy.minimall.stock.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存分页查询入参，支持按 SKU 过滤及仅查预警库存
 * @Version: 1.1
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class StockPageDTO extends CursorPageDTO {

    private Long skuId;

    /**
     * 是否仅查询已触发预警的库存（available &lt;= alertThreshold）。
     */
    private Boolean lowStockOnly;
}
