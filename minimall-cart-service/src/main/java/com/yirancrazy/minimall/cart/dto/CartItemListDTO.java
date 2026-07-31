package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItemList数据传输对象，用于CartItemList相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
public class CartItemListDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;
}