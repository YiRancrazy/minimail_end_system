package com.yirancrazy.minimall.order.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderItem持久化对象，映射t_order_item表，存储多SKU订单的明细行。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_item")
public class OrderItemPO extends BasePO {
    private Long orderId;
    private Long skuId;
    private String skuName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
