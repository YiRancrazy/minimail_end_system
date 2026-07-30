package com.yirancrazy.minimall.api.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
* 库存预占入参 DTO，承载 SKU 标识与数量。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveDTO {
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
}