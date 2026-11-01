package com.yirancrazy.minimall.pay.dto;

import com.yirancrazy.minimall.pay.constant.PayChannelEnum;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 接收用户侧创建支付所需的订单号、金额和渠道。
 * 字段带非空、正数及两位小数约束；用户与商家归属由请求头和订单快照决定。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCreateDto {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "支付金额最多10位整数且最多2位小数")
    private BigDecimal amount;

    private PayChannelEnum channel;
}
