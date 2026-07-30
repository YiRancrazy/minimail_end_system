package com.yirancrazy.minimall.pay.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundVO {
    private String refundNo;
    private String paymentNo;
    private BigDecimal amount;
    private Integer status;
}