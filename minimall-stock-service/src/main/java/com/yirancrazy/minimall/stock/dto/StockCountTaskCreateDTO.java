package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存盘点任务创建入参
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class StockCountTaskCreateDTO {

    @NotNull(message = "skuId cannot be null")
    private Long skuId;

    /** 备注 */
    private String remark;

    /** 操作人ID，由 Controller 从 Header 注入 */
    private Long operatorId;
}
