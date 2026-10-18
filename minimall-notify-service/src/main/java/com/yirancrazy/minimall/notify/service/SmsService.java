package com.yirancrazy.minimall.notify.service;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SmsService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface SmsService {


    /**
     * 发送业务消息。
     * @param phone phone 参数
     * @param templateCode templateCode 参数
     * @param params params 参数
     * @return 是否处理成功
     */
    boolean send(String phone, String templateCode, java.util.Map<String, String> params);
}
