package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载全选或取消全选所需的必填布尔状态，用于更新请求用户拥有的全部购物车条目。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CartSelectAllDto {

    @NotNull(message = "选中状态不能为空")
    private Boolean isSelected;
}
