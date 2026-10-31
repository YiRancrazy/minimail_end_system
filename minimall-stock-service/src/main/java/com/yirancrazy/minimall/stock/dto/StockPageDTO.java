package com.yirancrazy.minimall.stock.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义平台库存游标分页的 SKU 和低库存筛选条件。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockPageDto extends CursorPageDto {

  private Long skuId;

  private Boolean lowStockOnly;
}
