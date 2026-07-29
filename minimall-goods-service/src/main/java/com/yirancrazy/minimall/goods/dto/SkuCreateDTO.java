package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SKU creation request DTO.
 */
@Data
public class SkuCreateDTO {

    @NotNull(message = "spuId cannot be null")
    private Long spuId;

    @NotBlank(message = "skuName cannot be blank")
    @Size(min = 1, max = 200, message = "skuName length must be between 1 and 200")
    private String skuName;

    @Min(value = 0, message = "price must be non-negative")
    private BigDecimal price;

    @Min(value = 0, message = "stock must be non-negative")
    private Integer stock;
}