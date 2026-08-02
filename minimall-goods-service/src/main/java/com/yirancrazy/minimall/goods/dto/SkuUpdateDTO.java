package com.yirancrazy.minimall.goods.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Sku 修改入参，用于更新商品名称、价格与库存
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Data
public class SkuUpdateDTO {

    @NotBlank(message = "skuName cannot be blank")
    @Size(min = 1, max = 200, message = "skuName length must be between 1 and 200")
    private String skuName;

    @Min(value = 0, message = "price must be non-negative")
    private BigDecimal price;

    @Min(value = 0, message = "stock must be non-negative")
    private Integer stock;
}
