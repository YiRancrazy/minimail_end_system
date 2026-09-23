package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 数据传输对象，用于接收请求参数。
 * @Version: 1.1
 * @DateTime: 2026/7/31
 **/
@Data
@NoArgsConstructor
public class PayCallbackDTO {
    @NotBlank(message = "支付单号不能为空")
    private String paymentNo;
    private String tradeNo;
    private boolean success;
    private String channelResponse;
    @Digits(integer = 10, fraction = 2, message = "回调金额最多10位整数且最多2位小数")
    private BigDecimal totalAmount;

    public PayCallbackDTO(String paymentNo, String tradeNo, boolean success, String channelResponse) {
        this(paymentNo, tradeNo, success, channelResponse, null);
    }

    public PayCallbackDTO(String paymentNo, String tradeNo, boolean success,
                          String channelResponse, BigDecimal totalAmount) {
        this.paymentNo = paymentNo;
        this.tradeNo = tradeNo;
        this.success = success;
        this.channelResponse = channelResponse;
        this.totalAmount = totalAmount;
    }
}
