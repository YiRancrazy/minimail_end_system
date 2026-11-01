package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_order_payment_snapshot，保存订单支付侧的金额和方式快照。
 *   业务边界：该实体用于查询展示与对账辅助，不替代支付服务的支付流水表。
 *   使用场景：订单服务查询支付快照时读取。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_payment_snapshot")
public class OrderPaymentSnapshotPo extends BasePo {
    private Long orderId;
    private BigDecimal payAmount;
    private Integer payMethod;
    private LocalDateTime expireAt;
}
