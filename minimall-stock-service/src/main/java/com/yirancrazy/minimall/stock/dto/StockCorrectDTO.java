package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台人工纠正 SKU 可用库存的请求数据。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class StockCorrectDto {

    @NotNull(message = "correctQuantity cannot be null")
    @Min(value = 0, message = "correctQuantity must be >= 0")
    private Long correctQuantity;

    private String reason;
}
