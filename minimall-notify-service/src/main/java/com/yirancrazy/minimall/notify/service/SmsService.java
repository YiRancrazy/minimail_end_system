package com.yirancrazy.minimall.notify.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 短信发送服务接口，预留阿里云 SMS 接入点。本期由 MockSmsServiceImpl 打日志占位。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
public interface SmsService {

    /**
     * 发送短信。本期 Mock 实现仅打日志，不真实下发。
     * @param phone 手机号（明文，调用方负责脱敏日志）
     * @param templateCode 模板编码
     * @param params 模板参数，可为 null
     * @return 是否发送成功
     */
    boolean send(String phone, String templateCode, java.util.Map<String, String> params);
}
