package com.yirancrazy.minimall.api.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 结算链路使用的购物车条目快照，保留条目、SKU 与购买数量.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutItemDto {

    @NotNull(message = "购物车条目ID不能为空")
    private Long cartItemId;

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;
}
