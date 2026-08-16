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