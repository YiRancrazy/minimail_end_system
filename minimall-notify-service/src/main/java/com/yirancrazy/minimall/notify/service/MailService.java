package com.yirancrazy.minimall.notify.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 邮件发送服务接口，预留阿里云邮件推送接入点。本期由 MockMailServiceImpl 打日志占位。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
public interface MailService {

    /**
     * 发送邮件。本期 Mock 实现仅打日志，不真实下发。
     * @param to 收件邮箱
     * @param subject 主题
     * @param htmlBody HTML 正文
     * @return 是否发送成功
     */
    boolean send(String to, String subject, String htmlBody);
}
