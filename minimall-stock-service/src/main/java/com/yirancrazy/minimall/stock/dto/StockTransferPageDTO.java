package com.yirancrazy.minimall.stock.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义库存调拨记录的游标分页及源、目标 SKU 筛选条件。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockTransferPageDto extends CursorPageDto {

  private Long fromSkuId;
  private Long toSkuId;
}
