package com.yirancrazy.minimall.api.dto.stock;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 批量库存预占请求，一次调用完成同一结算单的 SKU 库存锁定
 * @Version: 1.0
 * @DateTime: 2026/09/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockBatchReserveDTO {

    @NotEmpty(message = "预占明细不能为空")
    @Size(max = 100, message = "单次最多预占100个SKU")
    @Valid
    private List<StockReserveDTO> items;
}
