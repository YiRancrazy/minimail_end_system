package com.yirancrazy.minimall.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaymentSnapshot持久化对象，映射t_order_payment_snapshot表，记录订单支付快照用于一致性校验。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_payment_snapshot")
public class OrderPaymentSnapshotPO extends BasePO {
    private Long orderId;
    private BigDecimal payAmount;
    private Integer payMethod;
    private LocalDateTime expireAt;
}
