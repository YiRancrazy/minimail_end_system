package com.yirancrazy.minimall.pay.service;

import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import java.math.BigDecimal;

public interface PayService {
    Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount);
    void handleCallback(PayCallbackDTO dto);
}