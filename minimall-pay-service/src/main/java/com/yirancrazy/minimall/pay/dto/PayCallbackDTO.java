package com.yirancrazy.minimall.pay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCallbackDTO {
    private String paymentNo;
    private String tradeNo;
    private boolean success;
    private String channelResponse;
}