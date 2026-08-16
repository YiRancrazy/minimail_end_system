package com.yirancrazy.minimall.pay.gateway;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
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
     * 创建电脑网站支付单（alipay.trade.page.pay）。
     * @param paymentNo 支付单号，作为 out_trade_no
     * @param amount 支付金额，单位元，必须大于 0
     * @param subject 订单标题
     * @param expireTime 绝对过期时间，格式 yyyy-MM-dd HH:mm:ss
     * @return 自动提交的收银台表单 HTML，前端写入页面后浏览器跳转支付宝收银台
     * @throws RuntimeException 支付宝网关不可达或参数非法时抛出
     */
    @Override
    public String createPagePayment(String paymentNo, BigDecimal amount, String subject, String expireTime) {

        if (paymentNo == null || paymentNo.isBlank() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid payment parameters");
        }

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 异步通知走 notify_url，支付完成后浏览器同步跳转 return_url，两者均由配置注入
        request.setNotifyUrl(config.getNotifyUrl());
        request.setReturnUrl(config.getReturnUrl());

        // 设置支付订单信息
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", paymentNo);
        bizContent.put("total_amount", amount.toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("time_expire", expireTime);
        // page.pay 必填产品码：电脑网站支付
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toJSONString());

        try {
            // pageExecute 本地组装参数并签名，返回带自动提交脚本的表单 HTML，无需服务端请求支付宝
            return alipayClient.pageExecute(request).getBody();
        }
        catch (AlipayApiException e) {
            log.error("create page payment failed, paymentNo={}", paymentNo, e);
            throw new RuntimeException("Alipay create page payment failed", e);
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