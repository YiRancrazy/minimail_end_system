package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_stock，维护单个 SKU 的商户归属、可用量、预占量和告警阈值。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock")
public class StockPo extends BasePo {
  private Long skuId;

  private Long merchantId;
  private Long available;
  private Long reserved;
  private Long alertThreshold;
}
