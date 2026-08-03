package com.yirancrazy.minimall.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 结算明细项DTO，单条SKU购买信息。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCheckoutItemDTO {

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;
}
