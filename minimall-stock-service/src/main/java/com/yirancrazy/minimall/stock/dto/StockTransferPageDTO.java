package com.yirancrazy.minimall.stock.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 跨商家库存调拨分页查询入参，支持按源/目标SKU过滤
 * @Version: 1.1
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class StockTransferPageDTO extends CursorPageDTO {

    private Long fromSkuId;
    private Long toSkuId;
}
