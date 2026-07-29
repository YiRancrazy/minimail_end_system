package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Cart item list query DTO.
 */
@Data
public class CartItemListDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;
}