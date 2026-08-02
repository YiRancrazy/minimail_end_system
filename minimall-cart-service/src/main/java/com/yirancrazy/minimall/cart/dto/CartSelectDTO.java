package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车项勾选状态修改入参，selected=1 选中、0 取消选中
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Data
public class CartSelectDTO {

    @NotNull(message = "选中状态不能为空")
    @Min(value = 0, message = "selected 只能为 0 或 1")
    @Max(value = 1, message = "selected 只能为 0 或 1")
    private Integer selected;
}
