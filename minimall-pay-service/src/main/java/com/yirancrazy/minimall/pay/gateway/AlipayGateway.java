package com.yirancrazy.minimall.pay.gateway;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.pay.config.AlipayConfig;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AlipayGateway 类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Slf4j
@Component
public class AlipayGateway implements PayGateway {

    private final AlipayClient alipayClient;    // 支付宝支付客户端
    private final AlipayConfig config;          // 支付宝支付配置

    public AlipayGateway(AlipayClient alipayClient, AlipayConfig config) {
        this.alipayClient = alipayClient;
        this.config = config;
    }

    /**
     * 创建支付订单（当面付预下单）。
     * @param paymentNo 支付订单号
     * @param amount 支付金额
     * @param subject 支付主题
     * @param expireTime 支付超时时间
     * @return 二维码内容（qr_code），前端据此生成二维码供用户扫码支付
     */
    @Override
    public String createPayment(String paymentNo, BigDecimal amount, String subject, String expireTime) {

        if (paymentNo == null || paymentNo.isBlank() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid payment parameters");
        }

        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(config.getNotifyUrl());    // 设置支付结果通知的地址

        // 设置支付订单信息
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", paymentNo);
        bizContent.put("total_amount", amount.toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("time_expire", expireTime);
        request.setBizContent(bizContent.toJSONString());

        try {
            // precreate 返回 qr_code 二维码内容，前端生成二维码图片，用户用支付宝扫码完成支付
            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                log.error("alipay precreate failed, paymentNo={}, code={}, subMsg={}",
                    paymentNo, response.getCode(), response.getSubMsg());
                throw new RuntimeException("Alipay precreate failed: " + response.getSubMsg());
            }
            return response.getQrCode();
        }
        catch (AlipayApiException e) {
            log.error("create payment failed, paymentNo={}", paymentNo, e);
            throw new RuntimeException("Alipay create payment failed", e);
        }
    }

    /**
     * 验证支付回调
     * @param params 支付回调参数
     * @return 支付订单号
     */
    @Override
    public String verifyCallback(Map<String, String> params) {
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                params, config.getAlipayPublicKey(), config.getCharset(), config.getSignType());
            if (!verified) {
                throw new RuntimeException("Alipay callback signature verification failed");
            }
            return params.get("trade_no");
        }
        catch (AlipayApiException e) {
            log.error("verify callback failed", e);
            throw new RuntimeException("Alipay callback verification failed", e);
        }
    }

    /**
     * 查询支付订单
     * @param paymentNo 支付订单号
     * @return 支付订单状态
     */
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
        }
        catch (AlipayApiException e) {
            log.error("query payment failed, paymentNo={}", paymentNo, e);
            return null;
        }
    }

    /**
     * 申请退款
     * @param paymentNo 支付订单号
     * @param refundNo 退款订单号
     * @param amount 退款金额
     * @param reason 退款原因
     * @return 退款订单号
     */
    @Override
    public String refund(String paymentNo, String refundNo, BigDecimal amount, String reason) {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizContent(String.format(
            "{\"out_trade_no\":\"%s\",\"refund_amount\":\"%s\",\"refund_reason\":\"%s\",\"out_request_no\":\"%s\"}",
            paymentNo, amount.toPlainString(), reason, refundNo));
        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getTradeNo();
            }
            throw new RuntimeException("Alipay refund failed: " + response.getSubMsg());
        }
        catch (AlipayApiException e) {
            log.error("refund failed, paymentNo={}, refundNo={}", paymentNo, refundNo, e);
            throw new RuntimeException("Alipay refund failed", e);
        }
    }
}