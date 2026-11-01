package com.yirancrazy.minimall.api.dto.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付服务回传订单服务的退款结果，包含本次退款、累计退款和幂等请求号.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundCallbackDto {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "支付流水ID不能为空")
    private Long payId;

    @NotNull(message = "本次退款金额不能为空")
    @DecimalMin(value = "0.00", message = "本次退款金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "本次退款金额最多两位小数")
    private BigDecimal refundedAmount;

    @NotNull(message = "累计退款金额不能为空")
    @DecimalMin(value = "0.00", message = "累计退款金额不能为负数")
    @Digits(integer = 10, fraction = 2, message = "累计退款金额最多两位小数")
    private BigDecimal totalRefundedAmount;

    @NotNull(message = "是否退清不能为空")
    private Boolean fullyRefunded;

    @NotBlank(message = "退款请求号不能为空")
    private String refundRequestNo;
}
