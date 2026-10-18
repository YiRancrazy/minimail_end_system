package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTaskCompleteDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class StockCountTaskCompleteDTO {

    @NotNull(message = "actualQuantity cannot be null")
    @Min(value = 0, message = "actualQuantity must be >= 0")
    private Long actualQuantity;


    private String remark;
}
