package com.yirancrazy.minimall.api.dto.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品服务向库存服务提交的库存增量调整请求
 * @Version: 1.0
 * @DateTime: 2026/09/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAdjustDTO {

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "调整数量不能为空")
    private Long quantity;

    @Size(max = 255, message = "调整原因不能超过255字符")
    private String reason;
}
