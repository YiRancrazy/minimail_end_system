package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在传输边界表示单次购物车勾选状态。校验要求值非空，并限制为持久化编码 0 或 1。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CartSelectDto {

    @NotNull(message = "选中状态不能为空")
    @Min(value = 0, message = "selected 只能为 0 或 1")
    @Max(value = 1, message = "selected 只能为 0 或 1")
    private Integer selected;
}
