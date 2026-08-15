package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车全选/取消全选入参，userId 由网关 X-User-Id 可信头注入，不入参
 * @Version: 1.2
 * @DateTime: 2026/08/13
 */
@Data
public class CartSelectAllDTO {

    @NotNull(message = "选中状态不能为空")
    private Boolean isSelected;
}
