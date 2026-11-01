package com.yirancrazy.minimall.api.dto.cart;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 按用户和购物车条目 ID 获取结算快照的请求.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutQueryDto {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotEmpty(message = "购物车条目不能为空")
    @Size(max = 100, message = "单次最多查询100个购物车条目")
    private List<@NotNull(message = "购物车条目ID不能为空") Long> itemIds;
}
