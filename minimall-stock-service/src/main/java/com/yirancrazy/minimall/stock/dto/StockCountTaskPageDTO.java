package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存盘点任务分页查询入参，支持按SKU和状态过滤
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class StockCountTaskPageDTO {

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;

    private Long skuId;
    private Integer status;
}
