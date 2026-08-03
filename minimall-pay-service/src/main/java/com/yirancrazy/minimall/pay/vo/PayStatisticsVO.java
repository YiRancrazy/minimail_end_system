package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付资金统计视图对象，含交易总额、退款总额、交易笔数。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayStatisticsVO {
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private Long transactionCount;
}
