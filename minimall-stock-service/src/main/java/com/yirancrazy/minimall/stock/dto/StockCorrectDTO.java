package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存纠正入参
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class StockCorrectDTO {

    @NotNull(message = "correctQuantity cannot be null")
    @Min(value = 0, message = "correctQuantity must be >= 0")
    private Long correctQuantity;

    /** 纠正原因 */
    private String reason;
}
