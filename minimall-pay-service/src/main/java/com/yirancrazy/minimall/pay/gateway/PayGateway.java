package com.yirancrazy.minimall.pay.gateway;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 抽象外部支付渠道的页面支付、验签、查询和退款能力。
 * 业务服务通过该接口隔离支付宝 SDK；实现负责协议细节，接口约定渠道状态语义。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PayGateway {

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 表示渠道退款查询归一化后的四种结果。
   * 供待退款恢复逻辑区分成功、处理中、渠道无记录和可重试失败。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  enum RefundQueryStatus {
    SUCCESS,
    PENDING,
    NOT_FOUND,
    FAILED
  }

  /**
   * 创建电脑网站支付单.
   *
   * @param paymentNo 支付单号
   * @param amount 支付金额
   * @param subject 订单标题
   * @param expireTime 过期时间
   * @return 收银台表单
   */
  String createPagePayment(String paymentNo, BigDecimal amount, String subject, String expireTime);

  /**
   * 校验支付回调签名.
   *
   * @param params 回调参数
   * @return 支付交易号
   */
  String verifyCallback(Map<String, String> params);

  /**
   * 查询支付状态.
   *
   * @param paymentNo 支付单号
   * @return 支付状态
   */
  String queryPayment(String paymentNo);

    /**
     * 关闭支付订单.
     * @param paymentNo 支付单号
     * @return 订单关闭结果
     */
  String closePayment(String paymentNo);

  /**
   * 发起支付退款.
   *
   * @param paymentNo 支付单号
   * @param refundNo 退款单号
   * @param amount 退款金额
   * @param reason 退款原因
   * @return 退款结果
   */
  String refund(String paymentNo, String refundNo, BigDecimal amount, String reason);

  /**
   * 查询退款结果.
   *
   * @param paymentNo 支付单号
   * @param refundNo 退款单号
   * @return 退款查询状态
   */
  RefundQueryStatus queryRefund(String paymentNo, String refundNo);
}
