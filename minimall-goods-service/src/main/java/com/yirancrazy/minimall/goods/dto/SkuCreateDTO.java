package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载商家创建 SKU 所需的父 SPU、规格名称、价格和初始库存。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class SkuCreateDto {

    @NotNull(message = "spuId cannot be null")
    private Long spuId;

    @NotBlank(message = "skuName cannot be blank")
    @Size(min = 1, max = 200, message = "skuName length must be between 1 and 200")
    private String skuName;

    @Min(value = 0, message = "price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "price must have at most 2 decimal places")
    private BigDecimal price;

    @Min(value = 0, message = "stock must be non-negative")
    private Integer stock;
}
