package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存盘点任务完成入参
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class StockCountTaskCompleteDTO {

    @NotNull(message = "actualQuantity cannot be null")
    @Min(value = 0, message = "actualQuantity must be >= 0")
    private Long actualQuantity;

    /** 备注 */
    private String remark;
}
