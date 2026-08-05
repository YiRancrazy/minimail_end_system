package com.yirancrazy.minimall.notify.service.impl;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.notify.service.SmsService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 短信服务 Mock 实现，仅打日志，不真实下发。切换阿里云 SMS 时新建阿里云实现并启用对应 Bean。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Slf4j
@Service
@ConditionalOnProperty(name = "minimall.notify.sms.enabled", havingValue = "false", matchIfMissing = true)
public class MockSmsServiceImpl implements SmsService {

    @Override
    public boolean send(String phone, String templateCode, Map<String, String> params) {
        log.info("mock sms sent, phone={}, template={}, params={}", maskPhone(phone), templateCode, params);
        return true;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
