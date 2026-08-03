package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 跨商家库存调拨入参，支持指定源/目标SKU及调拨数量
 * @Version: 1.0
 * @DateTime: 2026/08/03
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

    /** 调拨原因 */
    private String reason;

    /** 操作人ID，由 Controller 从 Header 注入 */
    private Long operatorId;
}
