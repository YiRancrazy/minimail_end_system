package com.yirancrazy.minimall.order.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPO description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class OrderPO extends BasePO {
    private Long userId;
    private Long merchantId;
    private Long payId;
    private Long skuId;
    private Integer quantity;
    private BigDecimal amount;
    private Integer status;
    private Integer refundFromStatus;
}