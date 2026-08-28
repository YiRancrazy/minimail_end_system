package com.yirancrazy.minimall.api.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockInit数据传输对象，商品服务创建SKU时联动初始化库存记录
 * @Version: 1.0
 * @DateTime: 2026/08/27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockInitDTO {
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    /** 初始可用数量，null 或负数按 0 处理 */
    @Min(value = 0, message = "初始库存不能为负数")
    private Integer initialQuantity;
}
