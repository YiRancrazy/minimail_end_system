package com.yirancrazy.minimall.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_sku 表，保存 SPU 下属规格、价格和库存展示缓存。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sku")
public class SkuPo extends BasePo {
  private Long spuId;
  // 归属商家ID：冗余自父SPU，用于商家侧越权校验
  private Long merchantId;
  private String skuName;
  private BigDecimal price;
  // 库存展示缓存，权威库存由库存服务的 t_stock.available 维护
  private Integer stock;
}
