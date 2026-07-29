package com.yirancrazy.minimall.api.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单已支付事件 DTO，用于通知他服务订单已支付并消费后续动作。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidDTO {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String paidAt;
}