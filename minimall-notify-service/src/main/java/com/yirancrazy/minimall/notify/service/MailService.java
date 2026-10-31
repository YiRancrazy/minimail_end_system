package com.yirancrazy.minimall.notify.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 抽象邮件发送能力；当前默认由 MockMailServiceImpl 在未启用真实邮件通道时记录脱敏日志.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface MailService {

  /**
   * 尝试发送一封 HTML 邮件，返回发送是否被当前实现接受.
   *
   * @param to 收件人邮箱地址
   * @param subject 邮件主题
   * @param htmlBody HTML 正文；日志实现只记录正文长度
   * @return true 表示发送成功或已被模拟实现接受
   */
  boolean send(String to, String subject, String htmlBody);
}
