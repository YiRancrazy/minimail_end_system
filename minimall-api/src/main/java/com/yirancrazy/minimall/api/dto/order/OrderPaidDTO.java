package com.yirancrazy.minimall.api.dto.order;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaid数据传输对象，用于OrderPaid相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class OrderPaidDTO {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String paidAt;
}