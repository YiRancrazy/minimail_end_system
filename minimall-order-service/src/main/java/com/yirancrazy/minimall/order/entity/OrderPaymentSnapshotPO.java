package com.yirancrazy.minimall.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaymentSnapshotPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
