package com.yirancrazy.minimall.pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Getter;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: AlipayConfig 配置类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
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
     * 设置相关数据。
     * @param appId appId 参数
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 设置相关数据。
     * @param privateKey privateKey 参数
     */
    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * 设置相关数据。
     * @param alipayPublicKey alipayPublicKey 参数
     */
    public void setAlipayPublicKey(String alipayPublicKey) {
        this.alipayPublicKey = alipayPublicKey;
    }

    /**
     * 设置相关数据。
     * @param gatewayUrl gatewayUrl 参数
     */
    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    /**
     * 设置相关数据。
     * @param notifyUrl notifyUrl 参数
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * 设置相关数据。
     * @param returnUrl returnUrl 参数
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    /**
     * 执行 alipayClient 方法。
     * @return 处理结果
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
            gatewayUrl, appId, privateKey, format, charset, alipayPublicKey, signType);
    }
}
