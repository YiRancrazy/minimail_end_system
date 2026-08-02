package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车项数量修改入参
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Data
public class CartUpdateDTO {

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;
}
