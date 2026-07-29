package com.yirancrazy.minimall.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付单持久化实体，对应 t_pay_record 表，承载订单标识、金额与状态等核心字段。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_pay_record")
public class PayRecordPO extends BasePO {
    private Long orderId;
    private BigDecimal amount;
    private String status;
}