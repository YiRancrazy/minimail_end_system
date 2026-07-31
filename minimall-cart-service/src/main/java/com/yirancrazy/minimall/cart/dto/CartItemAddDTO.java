package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItemAdd数据传输对象，用于CartItemAdd相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
public class CartItemAddDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;

    @NotNull(message = "skuId cannot be null")
    private Long skuId;

    @NotNull(message = "quantity cannot be null")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    private Integer selected;
}