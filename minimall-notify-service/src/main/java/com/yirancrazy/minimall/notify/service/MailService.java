package com.yirancrazy.minimall.notify.service;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MailService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface MailService {


    /**
     * 发送业务消息。
     * @param to to 参数
     * @param subject subject 参数
     * @param htmlBody htmlBody 参数
     * @return 是否处理成功
     */
    boolean send(String to, String subject, String htmlBody);
}
