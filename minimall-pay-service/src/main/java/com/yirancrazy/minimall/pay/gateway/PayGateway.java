package com.yirancrazy.minimall.pay.gateway;

import java.math.BigDecimal;
import java.util.Map;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 网关实现类，处理支付等外部接口调用。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
public interface PayGateway {

    /**
     * 退款渠道查询结果。
     */
    enum RefundQueryStatus {
        SUCCESS,
        PENDING,
        NOT_FOUND,
        FAILED
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
    String createPagePayment(String paymentNo, BigDecimal amount, String subject, String expireTime);

    /**
     * 校验回调签名并返回 trade_no。
     */
    String verifyCallback(Map<String, String> params);

    /**
     * 查询支付宝支付状态。
     */
    String queryPayment(String paymentNo);

    /**
     * 退款支付。
     */
    String refund(String paymentNo, String refundNo, BigDecimal amount, String reason);

    /**
     * 按原商户退款请求号查询退款结果，用于异常恢复与补偿。
     * @param paymentNo 原支付商户订单号
     * @param refundNo 原退款请求号，作为 out_request_no
     * @return 退款查询状态；渠道不可用或返回无法识别时为 FAILED
     */
    RefundQueryStatus queryRefund(String paymentNo, String refundNo);
}
