package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 跨商家库存调拨分页查询入参，支持按源/目标SKU过滤
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class StockTransferPageDTO {

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;

    private Long fromSkuId;
    private Long toSkuId;
}
