package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车项修改入参，支持部分更新（仅非空字段生效），isSelected 以布尔表达勾选态
 * @Version: 1.2
 * @DateTime: 2026/08/13
 */
@Data
public class CartUpdateDTO {

    @Min(value = 1, message = "数量至少为1")
    @Max(value = 999, message = "数量不能超过999")
    private Integer quantity;

    private Boolean isSelected;
}
