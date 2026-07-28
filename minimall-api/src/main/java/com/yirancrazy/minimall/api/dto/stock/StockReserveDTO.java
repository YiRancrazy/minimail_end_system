package com.yirancrazy.minimall.api.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockReserveDTO {
    private Long skuId;
    private Integer quantity;
}