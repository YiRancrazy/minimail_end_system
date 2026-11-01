package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义向购物车添加 SKU 的已校验载荷。SKU 和数量必填，数量限制为 1 至 999；
 *     selected 可选，CartService 会将空值默认处理为已勾选。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CartItemAddDto {

    @NotNull(message = "skuId cannot be null")
    private Long skuId;

    @NotNull(message = "quantity cannot be null")
    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 999, message = "quantity must not exceed 999")
    private Integer quantity;

    private Integer selected;
}
