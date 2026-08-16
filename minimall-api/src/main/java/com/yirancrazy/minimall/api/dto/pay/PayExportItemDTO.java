package com.yirancrazy.minimall.api.dto.pay;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付流水导出项跨服务 DTO，供平台端对账单导出 CSV 使用。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayExportItemDTO {

    private String paymentNo;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    private Integer channel;
    private Integer status;
    private LocalDateTime createTime;
}
