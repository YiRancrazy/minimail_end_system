package com.yirancrazy.minimall.api.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存预占入参 DTO，承载 SKU 标识与数量。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveDTO {
    private Long skuId;
    private Integer quantity;
}