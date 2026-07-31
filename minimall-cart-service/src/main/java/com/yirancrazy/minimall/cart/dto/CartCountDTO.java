package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartCount数据传输对象，用于CartCount相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class CartCountDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;
}