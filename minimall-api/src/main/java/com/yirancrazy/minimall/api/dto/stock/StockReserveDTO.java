package com.yirancrazy.minimall.api.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    private String orderNo;


    /**
     * 构造 StockReserveDTO 实例。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     */
    public StockReserveDTO(Long skuId, Integer quantity) {
        this(skuId, quantity, null);
    }
}
