package com.yirancrazy.minimall.notify.gateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyPushGateway 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface NotifyPushGateway {


    /**
     * 发送业务消息。
     * @param userId userId 参数
     * @param payload payload 参数
     */
    void send(Long userId, String payload);
}
