package com.yirancrazy.minimall.api.dto.pay;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付流水导出使用的最小明细，覆盖支付单、订单、金额和状态.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayExportItemDto {

    private String paymentNo;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    private Integer channel;
    private Integer status;
    private LocalDateTime createTime;
}
