package com.yirancrazy.minimall.api.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 批量查询返回的单个 SKU 可售库存快照.
 * @Version: 1.0
 * @DateTime: 2026/10/20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAvailabilityDto {

  private Long skuId;
  private Long available;
}
