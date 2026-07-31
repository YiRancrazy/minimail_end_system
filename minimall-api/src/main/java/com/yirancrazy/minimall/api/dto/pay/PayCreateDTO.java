package com.yirancrazy.minimall.api.dto.pay;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayCreate数据传输对象，用于PayCreate相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data

@AllArgsConstructor

@NoArgsConstructor
public class PayCreateDTO {
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
}