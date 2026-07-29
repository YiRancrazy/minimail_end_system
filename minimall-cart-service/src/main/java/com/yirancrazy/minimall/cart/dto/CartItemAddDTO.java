package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Cart item add request DTO.
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