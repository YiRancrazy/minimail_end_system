package com.yirancrazy.minimall.goods.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuItemDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class SkuItemDTO {


    private Long id;

    // 兼容前端字段名 spec，即规格描述，落库为 skuName
    @JsonAlias("spec")
    @Size(max = 200, message = "skuName length must be <= 200")
    private String skuName;

    @Min(value = 0, message = "price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "price must have at most 2 decimal places")
    private BigDecimal price;

    @Min(value = 0, message = "stock must be non-negative")
    private Integer stock;
}
