package com.yirancrazy.minimall.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在 t_cart_item 表中按用户和 SKU 持久化购物车记录，保存数量和整数勾选编码。
 *     该对象继承 BasePo 的审计、逻辑删除和版本字段，只用于持久化边界，不直接从控制器返回。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_cart_item")
public class CartItemPo extends BasePo {
  private Long userId;
  private Long skuId;
  private Integer quantity;
  private Integer selected;
}
