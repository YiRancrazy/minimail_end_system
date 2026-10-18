package com.yirancrazy.minimall.notify.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.notify.service.MailService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MockMailServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
@ConditionalOnProperty(name = "minimall.notify.mail.enabled", havingValue = "false", matchIfMissing = true)
public class MockMailServiceImpl implements MailService {

    /**
     * 发送业务消息。
     * @param to to 参数
     * @param subject subject 参数
     * @param htmlBody htmlBody 参数
     * @return 是否处理成功
     */
    @Override
    public boolean send(String to, String subject, String htmlBody) {
        log.info("mock 邮件已发送，to={}，subject={}，bodyLen={}", maskEmail(to), subject,
            htmlBody == null ? 0 : htmlBody.length());
        return true;
    }

    /**
     * 处理maskEmail。
     * @param email email
     * @return 处理结果
     */
    private String maskEmail(String email) {
        if (email == null || email.indexOf('@') < 1) {
            return "***";
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }
}
