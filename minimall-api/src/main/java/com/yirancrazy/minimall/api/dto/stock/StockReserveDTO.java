package com.yirancrazy.minimall.api.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data

@AllArgsConstructor

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockReserveDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@NoArgsConstructor
public class StockReserveDTO {
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @NotBlank(message = "订单号不能为空")
    @Size(max = 32, message = "订单号不能超过32个字符")
    private String orderNo;
}
