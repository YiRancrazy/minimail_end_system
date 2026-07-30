package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundVO {
    private String refundNo;
    private String paymentNo;
    private BigDecimal amount;
    private Integer status;
}