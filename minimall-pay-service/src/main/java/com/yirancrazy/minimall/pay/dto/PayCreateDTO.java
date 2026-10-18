package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.yirancrazy.minimall.pay.constant.PayChannelEnum;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayCreateDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCreateDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "支付金额最多10位整数且最多2位小数")
    private BigDecimal amount;

    private PayChannelEnum channel;
}
