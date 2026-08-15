package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付流水创建DTO，承载订单号、用户、商户与金额。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCreateDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于0")
    private BigDecimal amount;

    private PayChannelEnum channel;
}
