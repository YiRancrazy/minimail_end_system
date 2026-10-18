package com.yirancrazy.minimall.pay.gateway;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.pay.config.AlipayConfig;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: AlipayGateway 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class AlipayGateway implements PayGateway {

    private final AlipayClient alipayClient;    // 支付宝支付客户端
    private final AlipayConfig config;          // 支付宝支付配置

    /**
     * 构造 AlipayGateway 实例。
     * @param alipayClient alipayClient 参数
     * @param config config 参数
     */
    public AlipayGateway(AlipayClient alipayClient, AlipayConfig config) {
        this.alipayClient = alipayClient;
        this.config = config;
    }


    /**
     * 创建业务数据。
     * @param paymentNo paymentNo 参数
     * @param amount amount 参数
     * @param subject subject 参数
     * @param expireTime expireTime 参数
     * @return 处理结果
     */
    @Override
    public String createPagePayment(String paymentNo, BigDecimal amount, String subject, String expireTime) {

        if (paymentNo == null || paymentNo.isBlank() || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("支付参数非法");
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
            log.error("创建支付宝页面支付失败，paymentNo={}", paymentNo, e);
            throw new RuntimeException("支付宝创建页面支付失败", e);
        }
    }


    /**
     * 执行 verifyCallback 方法。
     * @param params params 参数
     * @return 处理结果
     */
    @Override
    public String verifyCallback(Map<String, String> params) {
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                params, config.getAlipayPublicKey(), config.getCharset(), config.getSignType());
            if (!verified) {
                throw new RuntimeException("支付宝回调验签失败");
            }
            return params.get("trade_no");
        }
        catch (AlipayApiException e) {
            log.error("回调验签失败", e);
            throw new RuntimeException("支付宝回调验证失败", e);
        }
    }


    /**
     * 执行 queryPayment 方法。
     * @param paymentNo paymentNo 参数
     * @return 处理结果
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
            log.error("查询支付失败，paymentNo={}", paymentNo, e);
            return null;
        }
    }


    /**
     * 执行 refund 方法。
     * @param paymentNo paymentNo 参数
     * @param refundNo refundNo 参数
     * @param amount amount 参数
     * @param reason reason 参数
     * @return 处理结果
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
            throw new RuntimeException("支付宝退款失败：" + response.getSubMsg());
        }
        catch (AlipayApiException e) {
            log.error("退款失败，paymentNo={}，refundNo={}", paymentNo, refundNo, e);
            throw new RuntimeException("支付宝退款失败", e);
        }
    }


    /**
     * 执行 queryRefund 方法。
     * @param paymentNo paymentNo 参数
     * @param refundNo refundNo 参数
     * @return 处理结果
     */
    @Override
    public RefundQueryStatus queryRefund(String paymentNo, String refundNo) {
        if (paymentNo == null || paymentNo.isBlank() || refundNo == null || refundNo.isBlank()) {
            return RefundQueryStatus.FAILED;
        }
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", paymentNo);
        bizContent.put("out_request_no", refundNo);
        request.setBizContent(bizContent.toJSONString());
        try {
            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                String refundAmount = response.getRefundAmount();
                if (refundAmount == null || refundAmount.isBlank()) {
                    return RefundQueryStatus.NOT_FOUND;
                }
                if ("REFUND_SUCCESS".equals(response.getRefundStatus())
                    || response.getGmtRefundPay() != null) {
                    return RefundQueryStatus.SUCCESS;
                }
                return RefundQueryStatus.PENDING;
            }
            if (isRefundNotFound(response.getSubCode())) {
                return RefundQueryStatus.NOT_FOUND;
            }
            log.warn("查询退款失败，paymentNo={}，refundNo={}，subCode={}，subMsg={}",
                paymentNo, refundNo, response.getSubCode(), response.getSubMsg());
            return RefundQueryStatus.FAILED;
        }
        catch (AlipayApiException e) {
            log.error("查询退款失败，paymentNo={}，refundNo={}", paymentNo, refundNo, e);
            return RefundQueryStatus.FAILED;
        }
    }

    /**
     * 校验退款NotFound。
     * @param subCode sub编码
     * @return 是否满足条件
     */
    private static boolean isRefundNotFound(String subCode) {
        return "ACQ.TRADE_NOT_EXIST".equals(subCode)
            || "ACQ.REFUND_NOT_EXIST".equals(subCode)
            || "TRADE_NOT_EXIST".equals(subCode);
    }
}
