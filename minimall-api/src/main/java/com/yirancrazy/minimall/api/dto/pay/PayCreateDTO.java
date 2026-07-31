package com.yirancrazy.minimall.api.dto.pay;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayCreateDTO description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class PayCreateDTO {
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
}