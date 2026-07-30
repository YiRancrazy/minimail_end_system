package com.yirancrazy.minimall.pay.gateway.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.yirancrazy.minimall.pay.config.AlipayConfig;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class AlipayGatewayImpl implements AlipayGateway {

    private final AlipayClient alipayClient;
    private final AlipayConfig config;

    public AlipayGatewayImpl(AlipayClient alipayClient, AlipayConfig config) {
        this.alipayClient = alipayClient;
        this.config = config;
    }

    @Override
    public String createPayment(String paymentNo, BigDecimal amount, String subject, String expireTime) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(config.getReturnUrl());
        request.setNotifyUrl(config.getNotifyUrl());
        request.setBizContent(String.format(
            "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\",\"time_expire\":\"%s\"}",
            paymentNo, amount.toPlainString(), subject, expireTime));
        try {
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("create payment failed, paymentNo={}", paymentNo, e);
            throw new RuntimeException("Alipay create payment failed", e);
        }
    }

    @Override
    public String verifyCallback(Map<String, String> params) {
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                params, config.getAlipayPublicKey(), config.getCharset(), config.getSignType());
            if (!verified) {
                throw new RuntimeException("Alipay callback signature verification failed");
            }
            return params.get("trade_no");
        } catch (AlipayApiException e) {
            log.error("verify callback failed", e);
            throw new RuntimeException("Alipay callback verification failed", e);
        }
    }

    @Override
    public String queryPayment(String paymentNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent(String.format("{\"out_trade_no\":\"%s\"}", paymentNo));
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getTradeStatus();
            }
            return null;
        } catch (AlipayApiException e) {
            log.error("query payment failed, paymentNo={}", paymentNo, e);
            return null;
        }
    }
}