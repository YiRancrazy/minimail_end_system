package com.yirancrazy.minimall.stock.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存盘点任务分页查询入参，支持按SKU和状态过滤
 * @Version: 1.1
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class StockCountTaskPageDTO extends CursorPageDTO {

    private Long skuId;
    private Integer status;
}
