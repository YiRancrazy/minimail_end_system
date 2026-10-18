package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PaymentParamsVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentParamsVO {
    private String paymentNo;
    private String orderNo;
    private BigDecimal amount;
    private String currency;
    private Integer channel;
    private String subject;
    private LocalDateTime expireAt;

    private String payForm;
}
