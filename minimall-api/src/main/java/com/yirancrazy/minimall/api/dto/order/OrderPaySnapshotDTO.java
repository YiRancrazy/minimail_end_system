package com.yirancrazy.minimall.api.dto.order;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单支付快照 DTO，承载支付服务创建支付单前的归属、金额和状态校验数据。
 * @Version: 1.0
 * @DateTime: 2026/09/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaySnapshotDTO {

    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    @Digits(integer = 10, fraction = 2, message = "payAmount must have at most 2 decimal places")
    private BigDecimal payAmount;
    private Integer status;
    private Long payId;
}
