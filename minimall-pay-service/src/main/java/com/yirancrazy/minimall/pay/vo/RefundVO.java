package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: RefundVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundVO {
    private String refundNo;
    private String paymentNo;
    private BigDecimal amount;
    private Integer status;
}