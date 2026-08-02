package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车清空入参
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Data
public class CartClearDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
