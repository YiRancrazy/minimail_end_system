package com.yirancrazy.minimall.pay.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 绑定 `minimall.alipay` 配置并创建支付宝客户端。
 * 配置包含应用身份、私钥、支付宝公钥及回调地址，供 `AlipayGateway` 发起签名和验签。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@Configuration
@ConfigurationProperties(prefix = "minimall.alipay")
public class AlipayConfig {

    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String gatewayUrl;
    private String notifyUrl;
    private String returnUrl;
    private String format = "json";
    private String charset = "UTF-8";
    private String signType = "RSA2";

    /**
     * 设置支付宝应用 ID，供配置绑定器在应用启动时注入。
     * 该方法只保存配置值，格式应与开放平台应用一致。
     *
     * @param appId 支付宝开放平台应用 ID
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 设置应用私钥，供支付宝请求签名使用。
     * 私钥属于敏感配置，不应出现在日志或返回对象中。
     *
     * @param privateKey 应用 RSA2 私钥
     */
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * 设置支付宝公钥，供异步回调验签和响应验签使用。
     *
     * @param alipayPublicKey 支付宝开放平台公钥
     */
    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    /**
     * 设置支付宝开放平台网关地址。
     * 网关地址决定 SDK 请求目标，测试与生产环境通过外部配置区分。
     *
     * @param gatewayUrl 支付宝网关 URL
     */
    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    /**
     * 设置支付宝异步通知地址，写入页面支付请求的 `notify_url`。
     *
     * @param notifyUrl 支付结果异步通知 URL
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * 设置支付完成后浏览器同步跳转地址，写入页面支付请求的 `return_url`。
     *
     * @param returnUrl 支付完成同步返回 URL
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    /**
     * 创建支付宝 SDK 客户端 Bean。
     * 客户端使用当前配置组装，不发起网络请求。
     *
     * @return 使用当前应用配置初始化的支付宝客户端
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                gatewayUrl, appId, privateKey, format, charset, alipayPublicKey, signType);
    }
}
