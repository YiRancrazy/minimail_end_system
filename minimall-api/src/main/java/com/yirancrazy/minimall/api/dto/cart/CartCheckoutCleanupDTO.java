package com.yirancrazy.minimall.api.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单结算成功后待清理的购物车条目集合.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutCleanupDto {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotEmpty(message = "购物车清理项不能为空")
    @Size(max = 100, message = "单次最多清理100个购物车条目")
    @Valid
    private List<CartCheckoutItemDto> items;
}
