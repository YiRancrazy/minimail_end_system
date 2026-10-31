package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_order_item，保存订单明细及下单时商品快照。
 *   业务边界：商品名称、价格、商家和 SPU/SKU 快照在落库后保持稳定；退款状态字段只表达明细退款状态。
 *   使用场景：订单创建、详情回显、库存释放和退款校验使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_item")
public class OrderItemPo extends BasePo {
  private Long orderId;
  private Long spuId;
  private Long merchantId;
  private String spuSnapshotJson;
  private String skuSnapshotJson;
  private String skuImageUrl;
  private Long skuId;
  private String skuName;
  private Integer quantity;
  private BigDecimal unitPrice;
  private BigDecimal amount;
  private BigDecimal subtotalAmount;
  private BigDecimal discountAmount;
  private BigDecimal payAmount;
  private Integer refundStatus;
}
