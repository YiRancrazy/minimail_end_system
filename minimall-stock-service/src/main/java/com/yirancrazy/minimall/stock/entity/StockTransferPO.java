package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_stock_transfer，保存成功库存调拨的来源、去向和操作审计信息。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock_transfer")
public class StockTransferPo extends BasePo {
  private Long fromSkuId;
  private Long toSkuId;
  private Long quantity;
  private String reason;
  private Long operatorId;
}
