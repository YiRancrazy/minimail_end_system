package com.yirancrazy.minimall.api.dto.order;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaid数据传输对象，用于OrderPaid相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data

@AllArgsConstructor

@NoArgsConstructor
public class OrderPaidDTO {
    private Long orderId;
    private Long userId;
    @Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
    private BigDecimal amount;
    private String paidAt;
}
