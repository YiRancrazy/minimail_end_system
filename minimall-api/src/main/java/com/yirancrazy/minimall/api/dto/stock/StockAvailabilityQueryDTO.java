package com.yirancrazy.minimall.api.dto.stock;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存可用量批量查询参数。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAvailabilityQueryDTO {

    @NotEmpty(message = "SKU ID列表不能为空")
    @Size(max = 500, message = "单次最多查询500个SKU")
    private List<@NotNull(message = "SKU ID不能为空") Long> skuIds;
}
