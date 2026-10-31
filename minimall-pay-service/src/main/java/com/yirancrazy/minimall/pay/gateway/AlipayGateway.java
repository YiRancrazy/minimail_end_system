package com.yirancrazy.minimall.pay.gateway;

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
import com.yirancrazy.minimall.pay.config.AlipayConfig;
import java.math.BigDecimal;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现支付宝电脑网站支付的网关适配。
 * 负责页面表单签名、回调验签、交易查询和退款；网络或协议异常在此转换并记录。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class AlipayGateway implements PayGateway {

  private final AlipayClient alipayClient; // 支付宝支付客户端
  private final AlipayConfig config; // 支付宝支付配置

  /**
   * 注入支付宝客户端和当前支付配置。
   *
   * @param alipayClient 负责签名及调用支付宝开放平台的 SDK 客户端
   * @param config 支付宝应用、网关和回调配置
   */
  public AlipayGateway(AlipayClient alipayClient, AlipayConfig config) {
    this.alipayClient = alipayClient;
    this.config = config;
  }

  /**
   * 创建电脑网站支付表单并返回可自动提交的 HTML。
   * 方法校验支付单号和正金额，将订单信息签名组装为 `FAST_INSTANT_TRADE_PAY` 请求。
   *
   * @param paymentNo 商户支付单号
   * @param amount 支付金额，必须大于零
   * @param subject 收银台展示标题
   * @param expireTime 支付宝要求格式的过期时间
   * @return 已签名、可由浏览器提交的支付表单 HTML
   * @throws IllegalArgumentException 支付单号为空或金额非正时抛出
   * @throws RuntimeException SDK 签名或页面表单生成失败时抛出
   */
  @Override
  public String createPagePayment(
      String paymentNo, BigDecimal amount, String subject, String expireTime) {

    if (paymentNo == null
        || paymentNo.isBlank()
        || amount == null
        || amount.compareTo(BigDecimal.ZERO) <= 0) {
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
    } catch (AlipayApiException e) {
      log.error("创建支付宝页面支付失败，paymentNo={}", paymentNo, e);
      throw new RuntimeException("支付宝创建页面支付失败", e);
    }
  }

  /**
   * 使用支付宝公钥验证异步通知签名。
   * 验签通过后返回支付宝交易号；验签失败不会继续处理支付状态。
   *
   * @param params 支付宝异步通知全部参数
   * @return 验签通过的支付宝交易号
   * @throws RuntimeException 验签不通过或 SDK 校验异常时抛出
   */
  @Override
  public String verifyCallback(Map<String, String> params) {
    try {
      boolean verified =
          AlipaySignature.rsaCheckV1(
              params, config.getAlipayPublicKey(), config.getCharset(), config.getSignType());
      if (!verified) {
        throw new RuntimeException("支付宝回调验签失败");
      }
      return params.get("trade_no");
    } catch (AlipayApiException e) {
      log.error("回调验签失败", e);
      throw new RuntimeException("支付宝回调验证失败", e);
    }
  }

  /**
   * 按商户支付单号查询支付宝交易状态。
   * 查询失败或 SDK 异常由该方法记录日志并返回 null，调用方需按未知状态处理。
   *
   * @param paymentNo 商户支付单号
   * @return 支付宝交易状态；查询失败时返回 null
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
    } catch (AlipayApiException e) {
      log.error("查询支付失败，paymentNo={}", paymentNo, e);
      return null;
    }
  }

  /**
   * 向支付宝发起指定支付单的部分或全额退款。
   * 以退款单号作为 `out_request_no` 保证渠道侧幂等。
   *
   * @param paymentNo 原商户支付单号
   * @param refundNo 商户退款单号
   * @param amount 本次退款金额
   * @param reason 退款原因
   * @return 支付宝交易号
   * @throws RuntimeException 渠道返回失败或 SDK 调用异常时抛出
   */
  @Override
  public String refund(String paymentNo, String refundNo, BigDecimal amount, String reason) {
    AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
    request.setBizContent(
        String.format(
            "{\"out_trade_no\":\"%s\",\"refund_amount\":\"%s\","
                + "\"refund_reason\":\"%s\",\"out_request_no\":\"%s\"}",
            paymentNo, amount.toPlainString(), reason, refundNo));
    try {
      AlipayTradeRefundResponse response = alipayClient.execute(request);
      if (response.isSuccess()) {
        return response.getTradeNo();
      }
      throw new RuntimeException("支付宝退款失败：" + response.getSubMsg());
    } catch (AlipayApiException e) {
      log.error("退款失败，paymentNo={}，refundNo={}", paymentNo, refundNo, e);
      throw new RuntimeException("支付宝退款失败", e);
    }
  }

  /**
   * 查询支付宝退款处理结果并归一化状态。
   * 空单号直接视为失败；渠道明确无记录时返回 `NOT_FOUND`，便于调用方重新发起。
   *
   * @param paymentNo 原商户支付单号
   * @param refundNo 商户退款单号
   * @return 成功、处理中、渠道无记录或失败
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
      log.warn(
          "查询退款失败，paymentNo={}，refundNo={}，subCode={}，subMsg={}",
          paymentNo,
          refundNo,
          response.getSubCode(),
          response.getSubMsg());
      return RefundQueryStatus.FAILED;
    } catch (AlipayApiException e) {
      log.error("查询退款失败，paymentNo={}，refundNo={}", paymentNo, refundNo, e);
      return RefundQueryStatus.FAILED;
    }
  }

  /**
   * 判断支付宝错误码是否表示交易或退款在渠道侧不存在。
   *
   * @param subCode 支付宝返回的业务错误码
   * @return 可安全重试退款的“不存在”错误码时返回 true
   */
  private static boolean isRefundNotFound(String subCode) {
    return "ACQ.TRADE_NOT_EXIST".equals(subCode)
        || "ACQ.REFUND_NOT_EXIST".equals(subCode)
        || "TRADE_NOT_EXIST".equals(subCode);
  }
}
