package com.yirancrazy.minimall.api.dto.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家对指定 SKU 库存进行增减调整并记录原因.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAdjustDto {

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "调整数量不能为空")
    private Long quantity;

    @Size(max = 255, message = "调整原因不能超过255字符")
    private String reason;
}
