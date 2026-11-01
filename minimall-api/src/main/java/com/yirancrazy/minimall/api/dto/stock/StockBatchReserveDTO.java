package com.yirancrazy.minimall.api.dto.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 一次请求预占多个 SKU 库存的明细集合.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockBatchReserveDto {

    @NotEmpty(message = "预占明细不能为空")
    @Size(max = 100, message = "单次最多预占100个SKU")
    @Valid
    private List<StockReserveDto> items;
}
