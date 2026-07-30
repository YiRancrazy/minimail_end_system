package com.yirancrazy.minimall.pay.gateway;

import java.math.BigDecimal;
import java.util.Map;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 网关实现类，处理支付等外部接口调用。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
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

    /**
     * Refund payment
     */
    String refund(String paymentNo, String refundNo, BigDecimal amount, String reason);
}