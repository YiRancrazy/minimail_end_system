package com.yirancrazy.minimall.stock.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_stock_journal，记录每次库存变动的类型、数量和业务来源。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_stock_journal")
public class StockJournalPo extends BasePo {
  private Long skuId;
  private Long quantity;
  private Integer type;
  private String reason;
  private String orderNo;
}
