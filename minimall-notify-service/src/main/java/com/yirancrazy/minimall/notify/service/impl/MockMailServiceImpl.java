package com.yirancrazy.minimall.notify.service.impl;

import com.yirancrazy.minimall.notify.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在邮件渠道未显式启用时提供日志型模拟发送，记录脱敏收件人、主题和正文长度，不调用外部邮件网关.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
@ConditionalOnProperty(
        name = "minimall.notify.mail.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class MockMailServiceImpl implements MailService {

    /**
     * 记录一条模拟邮件发送成功日志并返回成功；日志不输出完整收件人或正文.
     *
     * @param to 收件人邮箱地址
     * @param subject 邮件主题
     * @param htmlBody HTML 正文，仅记录非空长度
     * @return 固定返回 true
     */
    @Override
    public boolean send(String to, String subject, String htmlBody) {
        log.info(
                "mock 邮件已发送，to={}，subject={}，bodyLen={}",
                maskEmail(to),
                subject,
                htmlBody == null ? 0 : htmlBody.length());
        return true;
    }

    /**
     * 将邮箱转换为保留首字符和域名部分的日志脱敏格式；格式无效时返回 ***.
     *
     * @param email 原始邮箱地址
     * @return 脱敏邮箱地址
     */
    private String maskEmail(String email) {
        if (email == null || email.indexOf('@') < 1) {
            return "***";
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }
}
