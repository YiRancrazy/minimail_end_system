package com.yirancrazy.minimall.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_pay_record")
public class PayRecordPO extends BasePO {
    private Long orderId;
    private BigDecimal amount;
    private String status;
}