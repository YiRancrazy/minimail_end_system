package com.yirancrazy.minimall.api.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockInitDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockInitDTO {
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;


    @Min(value = 0, message = "初始库存不能为负数")
    private Integer initialQuantity;
}
