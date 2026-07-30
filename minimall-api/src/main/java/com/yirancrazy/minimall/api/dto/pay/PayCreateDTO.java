package com.yirancrazy.minimall.api.dto.pay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
* 创建支付单入参 DTO，承载订单标识与金额。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCreateDTO {
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
}