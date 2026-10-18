package com.yirancrazy.minimall.stock.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTaskCreateDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class StockCountTaskCreateDTO {

    @NotNull(message = "skuId cannot be null")
    private Long skuId;


    private String remark;


    private Long operatorId;
}
