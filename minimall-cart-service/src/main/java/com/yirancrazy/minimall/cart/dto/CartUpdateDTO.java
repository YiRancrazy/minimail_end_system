package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车项修改入参，支持部分更新（仅非空字段生效）
 * @Version: 1.1
 * @DateTime: 2026/08/04
 */
@Data
public class CartUpdateDTO {

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @Min(value = 0, message = "isSelected 只能为 0 或 1")
    @Max(value = 1, message = "isSelected 只能为 0 或 1")
    private Integer isSelected;
}
