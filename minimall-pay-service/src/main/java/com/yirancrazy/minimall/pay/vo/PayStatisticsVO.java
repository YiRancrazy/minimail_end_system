package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayStatisticsVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayStatisticsVO {
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private Long transactionCount;
}
