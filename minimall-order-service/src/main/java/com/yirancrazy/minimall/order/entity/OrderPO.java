package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class OrderPO extends BasePO {
    private Long userId;
    private Long payId;
    private Long skuId;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
}