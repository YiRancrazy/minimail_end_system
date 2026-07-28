package com.yirancrazy.minimall.api.dto.pay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCreateDTO {
    private Long orderId;
    private BigDecimal amount;
}