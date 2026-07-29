package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Cart count query DTO.
 */
@Data
public class CartCountDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;
}