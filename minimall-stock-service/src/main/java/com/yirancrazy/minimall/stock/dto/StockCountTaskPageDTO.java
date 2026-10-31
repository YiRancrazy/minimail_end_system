package com.yirancrazy.minimall.stock.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义盘点任务游标分页的 SKU 和状态筛选条件。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockCountTaskPageDto extends CursorPageDto {

  private Long skuId;
  private Integer status;
}
