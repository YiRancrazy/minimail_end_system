package com.yirancrazy.minimall.notify.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 抽象模板短信发送能力；当前默认由 MockSmsServiceImpl 记录脱敏手机号和模板参数.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface SmsService {

    /**
     * 使用模板编码和参数发送短信，返回发送是否被当前实现接受.
     *
     * @param phone 收件人手机号
     * @param templateCode 短信模板编码
     * @param params 模板变量集合
     * @return true 表示发送成功或已被模拟实现接受
     */
    boolean send(String phone, String templateCode, java.util.Map<String, String> params);
}
