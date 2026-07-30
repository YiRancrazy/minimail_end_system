package com.yirancrazy.minimall.pay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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