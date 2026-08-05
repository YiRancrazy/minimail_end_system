package com.yirancrazy.minimall.notify.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.notify.service.MailService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 邮件服务 Mock 实现，仅打日志，不真实下发。切换阿里云邮件推送时新建阿里云实现并启用对应 Bean。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Slf4j
@Service
@ConditionalOnProperty(name = "minimall.notify.mail.enabled", havingValue = "false", matchIfMissing = true)
public class MockMailServiceImpl implements MailService {

    @Override
    public boolean send(String to, String subject, String htmlBody) {
        log.info("mock mail sent, to={}, subject={}, bodyLen={}", maskEmail(to), subject,
            htmlBody == null ? 0 : htmlBody.length());
        return true;
    }

    private String maskEmail(String email) {
        if (email == null || email.indexOf('@') < 1) {
            return "***";
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }
}
