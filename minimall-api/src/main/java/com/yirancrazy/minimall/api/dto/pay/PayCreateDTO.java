package com.yirancrazy.minimall.api.dto.pay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 创建支付单入参 DTO，承载订单标识与金额。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCreateDTO {
    private Long orderId;
    private BigDecimal amount;
}