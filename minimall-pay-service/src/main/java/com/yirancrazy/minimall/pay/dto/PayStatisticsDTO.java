package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayStatisticsDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayStatisticsDTO {

    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private Long transactionCount;
}
