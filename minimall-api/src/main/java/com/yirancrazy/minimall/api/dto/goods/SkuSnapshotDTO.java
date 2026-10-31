package com.yirancrazy.minimall.api.dto.goods;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 跨服务传递的 SKU 下单快照，提供名称、价格、库存和商家归属.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkuSnapshotDto {
  private Long skuId;
  private Long spuId;
  private String skuName;
  private BigDecimal price;
  private Integer stock;
  private Long merchantId;
}
