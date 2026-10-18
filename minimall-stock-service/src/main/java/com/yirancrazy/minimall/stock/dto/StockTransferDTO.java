package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockTransferDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class StockTransferDTO {

    @NotNull(message = "fromSkuId cannot be null")
    private Long fromSkuId;

    @NotNull(message = "toSkuId cannot be null")
    private Long toSkuId;

    @NotNull(message = "quantity cannot be null")
    @Min(value = 1, message = "quantity must be >= 1")
    private Long quantity;


    private String reason;


    private Long operatorId;
}
