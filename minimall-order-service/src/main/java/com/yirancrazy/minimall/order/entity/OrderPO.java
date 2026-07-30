package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
* 订单持久化实体，对应订单表并承载支付标识、用户、商品、数量、金额及订单状态信息。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class OrderPO extends BasePO {
    private Long userId;
    private Long merchantId;
    private Long payId;
    private Long skuId;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
}