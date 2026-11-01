package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载创建 SKU 盘点任务所需的业务标识和操作信息。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class StockCountTaskCreateDto {

    @NotNull(message = "skuId cannot be null")
    private Long skuId;

    private String remark;

    private Long operatorId;
}
