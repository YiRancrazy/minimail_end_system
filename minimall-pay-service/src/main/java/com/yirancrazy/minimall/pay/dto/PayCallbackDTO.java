package com.yirancrazy.minimall.pay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 数据传输对象，用于接收请求参数。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCallbackDTO {
    @NotBlank(message = "支付单号不能为空")
    private String paymentNo;
    private String tradeNo;
    private boolean success;
    private String channelResponse;
}