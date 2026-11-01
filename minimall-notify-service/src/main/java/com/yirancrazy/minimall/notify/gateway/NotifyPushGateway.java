package com.yirancrazy.minimall.notify.gateway;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 抽象站内通知的实时推送通道，将通知服务与具体 SSE 连接管理实现解耦.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface NotifyPushGateway {

    /**
     * 向指定用户的已建立通知连接推送标题；没有在线连接时按 SSE 实现静默跳过.
     *
     * @param userId 接收通知的用户编号
     * @param payload 实时推送的展示内容
     */
    void send(Long userId, String payload);
}
