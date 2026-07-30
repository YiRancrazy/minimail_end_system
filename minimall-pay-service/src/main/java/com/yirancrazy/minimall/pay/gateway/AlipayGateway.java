package com.yirancrazy.minimall.pay.gateway;

import java.math.BigDecimal;
import java.util.Map;

public interface AlipayGateway {

    /**
     * Create payment order and return pay page URL
     */
    String createPayment(String paymentNo, BigDecimal amount, String subject, String expireTime);

    /**
     * Verify callback signature and return trade_no
     */
    String verifyCallback(Map<String, String> params);

    /**
     * Query payment status from Alipay
     */
    String queryPayment(String paymentNo);
}